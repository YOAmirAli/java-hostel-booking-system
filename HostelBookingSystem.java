import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * SEN-311, Assignment 1: Aurora University Hostel Booking (single-block prototype).
 * Java standard library only; all inventory, payments and time are simulated in memory.
 *
 * Demo:      java HostelBookingSystem --demo
 * Self-test: java HostelBookingSystem --self-test
 * Menu:      java HostelBookingSystem
 */
public class HostelBookingSystem {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final LocalDateTime START = LocalDateTime.of(2026, 9, 23, 9, 0);

    enum RoomType { SINGLE, DOUBLE, SHARED }
    enum BookingStatus { PENDING_DEPOSIT, CONFIRMED, EXPIRED, CANCELLED, SWITCHED }

    static final class Student {
        final String rollNumber;
        final boolean requiresAccessibleRoom;
        String activeBookingReference;

        Student(String rollNumber, boolean requiresAccessibleRoom) {
            this.rollNumber = rollNumber;
            this.requiresAccessibleRoom = requiresAccessibleRoom;
        }
    }

    static final class Room {
        final String id;
        final String block;
        final RoomType type;
        final int capacity;
        final boolean accessible;
        String occupiedByBooking;

        Room(String id, String block, RoomType type, int capacity, boolean accessible) {
            this.id = id;
            this.block = block;
            this.type = type;
            this.capacity = capacity;
            this.accessible = accessible;
        }

        boolean available() { return occupiedByBooking == null; }
    }

    static final class Booking {
        final String reference;
        final String studentRoll;
        final String roomId;
        final LocalDateTime createdAt;
        final LocalDateTime depositDueAt;
        BookingStatus status;
        LocalDateTime depositPaidAt;

        Booking(String reference, String studentRoll, String roomId,
                LocalDateTime createdAt, LocalDateTime depositDueAt,
                BookingStatus status, LocalDateTime depositPaidAt) {
            this.reference = reference;
            this.studentRoll = studentRoll;
            this.roomId = roomId;
            this.createdAt = createdAt;
            this.depositDueAt = depositDueAt;
            this.status = status;
            this.depositPaidAt = depositPaidAt;
        }

        boolean active() {
            return status == BookingStatus.PENDING_DEPOSIT || status == BookingStatus.CONFIRMED;
        }
    }

    static final class Result {
        final boolean success;
        final String message;
        final String bookingReference;

        Result(boolean success, String message, String bookingReference) {
            this.success = success;
            this.message = message;
            this.bookingReference = bookingReference;
        }

        @Override public String toString() {
            return (success ? "SUCCESS: " : "REJECTED: ") + message
                    + (bookingReference == null ? "" : " [" + bookingReference + "]");
        }
    }

    static final class RequestRecord {
        final String payload;
        final Result result;
        RequestRecord(String payload, Result result) {
            this.payload = payload;
            this.result = result;
        }
    }

    /**
     * The synchronized methods create a single critical section for the in-memory
     * check + allocation + record update; a production DB would need transactions
     * and a unique constraint, not just this process-local lock.
     */
    static final class BookingService {
        final String block;
        final Map<String, Student> students = new LinkedHashMap<String, Student>();
        final Map<String, Room> rooms = new LinkedHashMap<String, Room>();
        final Map<String, Booking> bookings = new LinkedHashMap<String, Booking>();
        final Map<String, RequestRecord> requestHistory = new LinkedHashMap<String, RequestRecord>();
        final List<String> audit = new ArrayList<String>();
        LocalDateTime now;
        int sequence = 1;

        BookingService(String block, Collection<Room> seedRooms) {
            this.block = block;
            this.now = START;
            for (Room room : seedRooms) {
                if (!block.equals(room.block) || rooms.put(room.id, room) != null) {
                    throw new IllegalArgumentException("Room block/ID invalid: " + room.id);
                }
            }
        }

        synchronized Result register(String roll, boolean accessible) {
            if (roll == null || roll.trim().isEmpty()) return fail("Roll number is required.");
            roll = roll.trim();
            Student previous = students.get(roll);
            if (previous != null) {
                if (previous.requiresAccessibleRoom != accessible)
                    return fail("Registration already exists; accessibility setting cannot silently change.");
                return ok("Student is already registered.", null);
            }
            students.put(roll, new Student(roll, accessible));
            log("STUDENT_REGISTERED", roll, "Accessibility requirement=" + accessible);
            return ok("Registered student " + roll + ".", null);
        }

        synchronized Result book(String roll, RoomType type, String requestId) {
            expirePending();
            String key = requestKey(roll, "BOOK", requestId);
            if (key == null) return fail("A request ID is required; no booking was created.");
            String payload = String.valueOf(type);
            Result replay = replay(key, payload);
            if (replay != null) return replay;
            Result outcome;
            Student student = students.get(roll);
            if (student == null) outcome = fail("Student is not registered.");
            else if (type == null) outcome = fail("Room type is invalid.");
            else if (student.activeBookingReference != null)
                outcome = fail("Only one active booking is allowed: " + student.activeBookingReference);
            else if (student.requiresAccessibleRoom)
                outcome = fail("Accessible accommodation is in Block C; this prototype serves Block " + block + ".");
            else {
                Room available = findAvailable(type, null);
                if (available == null) outcome = fail("No available " + type + " room in Block " + block + ".");
                else {
                    Booking booking = createBooking(student, available, now.plusHours(48),
                            BookingStatus.PENDING_DEPOSIT, null);
                    log("BOOKED", roll, "Room=" + available.id + "; ref=" + booking.reference
                            + "; deposit due=" + fmt(booking.depositDueAt));
                    outcome = ok("Room " + available.id + " reserved; deposit due "
                            + fmt(booking.depositDueAt) + ".", booking.reference);
                }
            }
            requestHistory.put(key, new RequestRecord(payload, outcome));
            return outcome;
        }

        synchronized Result payDeposit(String roll, String requestId) {
            expirePending();
            String key = requestKey(roll, "PAY", requestId);
            if (key == null) return fail("A request ID is required.");
            Result replay = replay(key, "PAY_DEPOSIT");
            if (replay != null) return replay;
            Student student = students.get(roll);
            Booking b = activeBooking(student);
            Result outcome;
            if (b == null) outcome = fail("No active booking; payment was not recorded.");
            else if (b.status == BookingStatus.CONFIRMED)
                outcome = ok("Deposit was already recorded; no second payment was made.", b.reference);
            else {
                b.depositPaidAt = now;
                b.status = BookingStatus.CONFIRMED;
                log("PAYMENT_RECEIVED", roll, "Room=" + b.roomId + "; ref=" + b.reference);
                outcome = ok("Deposit recorded; booking confirmed.", b.reference);
            }
            requestHistory.put(key, new RequestRecord("PAY_DEPOSIT", outcome));
            return outcome;
        }

        synchronized Result cancel(String roll, String requestId) {
            expirePending();
            String key = requestKey(roll, "CANCEL", requestId);
            if (key == null) return fail("A request ID is required.");
            Result replay = replay(key, "CANCEL_ACTIVE");
            if (replay != null) return replay;
            Student student = students.get(roll);
            Booking b = activeBooking(student);
            Result outcome;
            if (b == null) outcome = fail("No active booking to cancel.");
            else {
                b.status = BookingStatus.CANCELLED;
                rooms.get(b.roomId).occupiedByBooking = null;
                student.activeBookingReference = null;
                log("CANCELLED", roll, "Room=" + b.roomId + "; ref=" + b.reference);
                outcome = ok("Cancelled and released room " + b.roomId + ".", b.reference);
            }
            requestHistory.put(key, new RequestRecord("CANCEL_ACTIVE", outcome));
            return outcome;
        }

        synchronized Result switchRoom(String roll, RoomType newType, String requestId) {
            expirePending();
            String key = requestKey(roll, "SWITCH", requestId);
            if (key == null) return fail("A request ID is required.");
            String payload = String.valueOf(newType);
            Result replay = replay(key, payload);
            if (replay != null) return replay;
            Student student = students.get(roll);
            Booking previous = activeBooking(student);
            Result outcome;
            if (previous == null) outcome = fail("No active booking to switch.");
            else if (newType == null) outcome = fail("New room type is invalid.");
            else {
                // Reserve the replacement before releasing the old room; no partial switch.
                Room replacement = findAvailable(newType, previous.roomId);
                if (replacement == null) outcome = fail("No replacement room is available; original booking is unchanged.");
                else {
                    previous.status = BookingStatus.SWITCHED;
                    rooms.get(previous.roomId).occupiedByBooking = null;
                    student.activeBookingReference = null;
                    BookingStatus status = previous.depositPaidAt == null
                            ? BookingStatus.PENDING_DEPOSIT : BookingStatus.CONFIRMED;
                    // Preserve prior deposit payment, or the original 48-hour deadline.
                    Booking next = createBooking(student, replacement, previous.depositDueAt,
                            status, previous.depositPaidAt);
                    log("SWITCHED", roll, "Old=" + previous.roomId + "/" + previous.reference
                            + "; new=" + replacement.id + "/" + next.reference
                            + "; status=" + status);
                    outcome = ok("Switched from " + previous.roomId + " to " + replacement.id
                            + "; payment/deadline preserved.", next.reference);
                }
            }
            requestHistory.put(key, new RequestRecord(payload, outcome));
            return outcome;
        }

        synchronized Result lookupRequest(String roll, String requestId) {
            expirePending();
            String key = requestKey(roll, "BOOK", requestId);
            if (key == null) return fail("Request ID is required.");
            RequestRecord record = requestHistory.get(key);
            if (record == null) return fail("No booking request found with that ID.");
            if (record.result.bookingReference == null) return record.result;
            Booking booking = bookings.get(record.result.bookingReference);
            return ok("Original request found: room " + booking.roomId + ", current status="
                    + booking.status + ", original reference=" + booking.reference + ".", booking.reference);
        }

        synchronized Result bookingStatus(String roll) {
            expirePending();
            Student s = students.get(roll);
            if (s == null) return fail("Student is not registered.");
            Booking active = activeBooking(s);
            if (active != null) return ok(detail(active), active.reference);
            Booking recent = null;
            for (Booking b : bookings.values()) if (roll.equals(b.studentRoll)) recent = b;
            return recent == null ? fail("No booking history found.")
                    : ok("No active reservation. Latest booking: " + detail(recent), recent.reference);
        }

        synchronized void advanceHours(long hours) {
            if (hours <= 0 || hours > 100000) throw new IllegalArgumentException("Hours must be 1..100000.");
            now = now.plusHours(hours);
            log("CLOCK_ADVANCED", "SYSTEM", "Advanced " + hours + " h; now=" + fmt(now));
            expirePending();
        }

        synchronized String availability() {
            expirePending();
            StringBuilder sb = new StringBuilder("Block " + block + " | simulated clock: " + fmt(now) + "\n");
            for (RoomType type : RoomType.values()) {
                int total = 0, free = 0;
                for (Room room : rooms.values()) if (room.type == type) {
                    total++;
                    if (room.available()) free++;
                }
                sb.append(String.format("%-8s %2d / %2d free\n", type, free, total));
            }
            return sb.toString();
        }

        synchronized void printAudit() {
            expirePending();
            System.out.println("AUDIT TRAIL (" + audit.size() + " events)");
            for (String entry : audit) System.out.println(entry);
        }

        synchronized int activeCount(String roll) {
            expirePending();
            int count = 0;
            for (Booking b : bookings.values()) if (roll.equals(b.studentRoll) && b.active()) count++;
            return count;
        }

        synchronized int freeCount(RoomType type) {
            expirePending();
            int count = 0;
            for (Room r : rooms.values()) if (r.type == type && r.available()) count++;
            return count;
        }

        synchronized Booking getBooking(String reference) {
            expirePending();
            return bookings.get(reference);
        }

        private Booking createBooking(Student s, Room room, LocalDateTime deadline,
                                      BookingStatus status, LocalDateTime paidAt) {
            String ref = String.format("BKG-%04d", sequence++);
            Booking b = new Booking(ref, s.rollNumber, room.id, now, deadline, status, paidAt);
            bookings.put(ref, b);
            room.occupiedByBooking = ref;
            s.activeBookingReference = ref;
            return b;
        }

        private Booking activeBooking(Student student) {
            if (student == null || student.activeBookingReference == null) return null;
            Booking b = bookings.get(student.activeBookingReference);
            if (b == null || !b.active()) throw new IllegalStateException("Student booking index is inconsistent.");
            return b;
        }

        private Room findAvailable(RoomType type, String excludedRoom) {
            for (Room room : rooms.values()) {
                if (room.type == type && room.available() && !room.id.equals(excludedRoom)) return room;
            }
            return null;
        }

        private void expirePending() {
            for (Booking b : bookings.values()) {
                if (b.status == BookingStatus.PENDING_DEPOSIT && !now.isBefore(b.depositDueAt)) {
                    b.status = BookingStatus.EXPIRED;
                    Room room = rooms.get(b.roomId);
                    if (b.reference.equals(room.occupiedByBooking)) room.occupiedByBooking = null;
                    Student s = students.get(b.studentRoll);
                    if (b.reference.equals(s.activeBookingReference)) s.activeBookingReference = null;
                    log("RELEASED_UNPAID", b.studentRoll, "Room=" + b.roomId + "; ref=" + b.reference);
                }
            }
        }

        private String requestKey(String roll, String action, String requestId) {
            if (roll == null || roll.trim().isEmpty() || requestId == null || requestId.trim().isEmpty()) return null;
            return roll.trim() + "|" + action + "|" + requestId.trim();
        }

        private Result replay(String key, String payload) {
            RequestRecord prior = requestHistory.get(key);
            if (prior == null) return null;
            if (!prior.payload.equals(payload)) return fail("Request ID reused with different input; nothing changed.");
            return new Result(prior.result.success, "REPLAY (no duplicate action): " + prior.result.message,
                    prior.result.bookingReference);
        }

        private void log(String action, String actor, String info) {
            audit.add("[" + fmt(now) + "] " + action + " | " + actor + " | " + info);
        }

        private String detail(Booking b) {
            return "ref=" + b.reference + ", room=" + b.roomId + ", status=" + b.status
                    + ", created=" + fmt(b.createdAt) + ", deposit due=" + fmt(b.depositDueAt)
                    + ", paid=" + (b.depositPaidAt == null ? "not yet" : fmt(b.depositPaidAt));
        }
    }

    static String fmt(LocalDateTime time) { return DATE_FORMAT.format(time); }
    static Result ok(String message, String ref) { return new Result(true, message, ref); }
    static Result fail(String message) { return new Result(false, message, null); }

    static List<Room> blockARooms() {
        List<Room> list = new ArrayList<Room>();
        for (int i = 1; i <= 40; i++) list.add(new Room(String.format("A-S%02d", i), "A", RoomType.SINGLE, 1, false));
        for (int i = 1; i <= 30; i++) list.add(new Room(String.format("A-D%02d", i), "A", RoomType.DOUBLE, 2, false));
        for (int i = 1; i <= 20; i++) list.add(new Room(String.format("A-H%02d", i), "A", RoomType.SHARED, 4, false));
        return list;
    }

    static BookingService tinyService(int single, int doubles) {
        List<Room> rs = new ArrayList<Room>();
        for (int i = 1; i <= single; i++) rs.add(new Room("A-S" + i, "A", RoomType.SINGLE, 1, false));
        for (int i = 1; i <= doubles; i++) rs.add(new Room("A-D" + i, "A", RoomType.DOUBLE, 2, false));
        return new BookingService("A", rs);
    }

    static void demo() {
        System.out.println("=== SEN-311 HOSTEL BOOKING: REPRODUCIBLE DEMONSTRATION ===");
        System.out.println("Note: timestamps use a simulation clock; deposit payment is not real money.\n");
        BookingService a = tinyService(1, 0);
        a.register("S101", false); a.register("S102", false);
        System.out.println("SCENARIO 1 - Competing attempts for last single room");
        System.out.println(a.book("S101", RoomType.SINGLE, "req-1"));
        System.out.println(a.book("S102", RoomType.SINGLE, "req-2"));
        System.out.println(a.availability());
        System.out.println("SCENARIO 2 - Connection lost and same request retried");
        System.out.println(a.book("S101", RoomType.SINGLE, "req-1"));
        System.out.println(a.lookupRequest("S101", "req-1"));
        System.out.println("Active bookings for S101: " + a.activeCount("S101") + "\n");
        System.out.println("SCENARIO 3 - Unpaid reservation expires after 48 hours");
        a.advanceHours(49);
        System.out.println(a.bookingStatus("S101"));
        System.out.println(a.book("S102", RoomType.SINGLE, "req-3"));
        System.out.println(a.availability());
        System.out.println("SCENARIO 4 - Paid bookings remain confirmed; payment retry is safe");
        System.out.println(a.payDeposit("S102", "pay-1"));
        System.out.println(a.payDeposit("S102", "pay-1"));
        a.advanceHours(49);
        System.out.println(a.bookingStatus("S102"));
        System.out.println("SCENARIO 5 - Cancellation returns room to inventory");
        System.out.println(a.cancel("S102", "cancel-1"));
        System.out.println(a.availability());
        BookingService b = tinyService(1, 1);
        b.register("S201", false);
        System.out.println("SCENARIO 6 - Switching room preserves unpaid deposit deadline");
        System.out.println(b.book("S201", RoomType.SINGLE, "req-4"));
        b.advanceHours(24);
        System.out.println(b.switchRoom("S201", RoomType.DOUBLE, "switch-1"));
        System.out.println(b.bookingStatus("S201"));
        b.advanceHours(24);
        System.out.println(b.bookingStatus("S201"));
        System.out.println(b.availability());
        System.out.println("SCENARIO 7 - Timestamped audit trails");
        a.printAudit();
        System.out.println();
        b.printAudit();
        System.out.println("\n=== END DEMONSTRATION ===");
    }

    private static int testsPassed;
    static void check(String name, boolean condition) {
        if (!condition) throw new AssertionError("FAIL: " + name);
        testsPassed++;
        System.out.println("PASS: " + name);
    }

    static void selfTest() {
        testsPassed = 0;
        BookingService s = tinyService(1, 1);
        s.register("X", false); s.register("Y", false); s.register("Z", false);
        Result first = s.book("X", RoomType.SINGLE, "b1");
        check("first booking succeeds", first.success);
        check("second student cannot claim same last room", !s.book("Y", RoomType.SINGLE, "b2").success);
        check("one-active-reservation rule", !s.book("X", RoomType.DOUBLE, "b3").success && s.activeCount("X") == 1);
        Result repeat = s.book("X", RoomType.SINGLE, "b1");
        check("booking replay returns identical reference", repeat.success && first.bookingReference.equals(repeat.bookingReference) && s.activeCount("X") == 1);
        check("reuse request ID with changed room type refused", !s.book("X", RoomType.DOUBLE, "b1").success);
        check("failed confirmations can be looked up", first.bookingReference.equals(s.lookupRequest("X", "b1").bookingReference));
        s.advanceHours(47);
        check("booking still pending before 48 hours", s.getBooking(first.bookingReference).status == BookingStatus.PENDING_DEPOSIT);
        s.advanceHours(1);
        check("unpaid booking expires at 48 hours", s.getBooking(first.bookingReference).status == BookingStatus.EXPIRED && s.freeCount(RoomType.SINGLE) == 1 && s.activeCount("X") == 0);
        check("payment after expiration fails", !s.payDeposit("X", "late-pay").success);
        Result second = s.book("Y", RoomType.SINGLE, "b4");
        check("expired room can be booked by someone else", second.success);
        check("payment recorded once", s.payDeposit("Y", "p1").success && s.payDeposit("Y", "p1").success);
        s.advanceHours(49);
        check("paid reservation survives deadline", s.getBooking(second.bookingReference).status == BookingStatus.CONFIRMED && s.freeCount(RoomType.SINGLE) == 0);
        Result switched = s.switchRoom("Y", RoomType.DOUBLE, "sw1");
        check("successful switch swaps occupied room", switched.success && s.freeCount(RoomType.SINGLE) == 1 && s.freeCount(RoomType.DOUBLE) == 0 && s.activeCount("Y") == 1);
        check("paid status carries through switch", s.getBooking(switched.bookingReference).status == BookingStatus.CONFIRMED);
        check("switch replay does not create another booking", switched.bookingReference.equals(s.switchRoom("Y", RoomType.DOUBLE, "sw1").bookingReference));
        check("cancellation releases room", s.cancel("Y", "c1").success && s.freeCount(RoomType.DOUBLE) == 1 && s.activeCount("Y") == 0);
        check("cancellation replay is safe", s.cancel("Y", "c1").success);
        check("audit trail includes release, payment and cancellation", containsAudit(s, "RELEASED_UNPAID") && containsAudit(s, "PAYMENT_RECEIVED") && containsAudit(s, "CANCELLED"));
        BookingService p = tinyService(1, 1);
        p.register("P", false);
        p.book("P", RoomType.SINGLE, "book-a");
        p.advanceHours(24);
        Result unpaidSwitch = p.switchRoom("P", RoomType.DOUBLE, "switch-a");
        p.advanceHours(24);
        check("switching an unpaid room cannot reset 48-hour timer", p.getBooking(unpaidSwitch.bookingReference).status == BookingStatus.EXPIRED);
        BookingService access = tinyService(1, 0);
        access.register("ACCESS", true);
        check("Block A rejects accessible-room requirements", !access.book("ACCESS", RoomType.SINGLE, "access-1").success);
        check("expected Block A inventory is seeded", new BookingService("A", blockARooms()).freeCount(RoomType.SINGLE) == 40);
        System.out.println("ALL " + testsPassed + " CHECKS PASSED.");
    }

    static boolean containsAudit(BookingService service, String token) {
        for (String entry : service.audit) if (entry.contains(token)) return true;
        return false;
    }

    private static String prompt(Scanner scanner, String caption) {
        System.out.print(caption);
        return scanner.nextLine().trim();
    }

    static void menu() {
        BookingService service = new BookingService("A", blockARooms());
        Scanner scanner = new Scanner(System.in);
        System.out.println("AURORA HOSTEL - BLOCK A (simulation; data resets on restart)");
        System.out.println("Each operation requires a request ID; reuse the SAME ID after connection loss.");
        while (true) {
            System.out.println("\n1 Register student | 2 Availability | 3 Book | 4 Deposit | 5 Status");
            System.out.println("6 Cancel | 7 Switch | 8 Advance clock (hours) | 9 Booking request lookup");
            System.out.println("10 Audit | 11 Demonstration | 12 Launch GUI | 0 Exit");
            String option = prompt(scanner, "Choose: ");
            if ("0".equals(option)) break;
            try {
                switch (option) {
                    case "1": {
                        String roll = prompt(scanner, "Student roll number: ");
                        boolean accessible = prompt(scanner, "Requires accessible accommodation? (y/n): ")
                                .equalsIgnoreCase("y");
                        System.out.println(service.register(roll, accessible)); break;
                    }
                    case "2": System.out.println(service.availability()); break;
                    case "3": {
                        String roll = prompt(scanner, "Student roll number: ");
                        RoomType type = RoomType.valueOf(prompt(scanner, "Room type (SINGLE/DOUBLE/SHARED): ").toUpperCase());
                        String key = prompt(scanner, "Unique request ID (e.g. req-101): ");
                        System.out.println(service.book(roll, type, key)); break;
                    }
                    case "4": {
                        String roll = prompt(scanner, "Student roll number: ");
                        System.out.println(service.payDeposit(roll, prompt(scanner, "Payment request ID: "))); break;
                    }
                    case "5": System.out.println(service.bookingStatus(prompt(scanner, "Student roll number: "))); break;
                    case "6": {
                        String roll = prompt(scanner, "Student roll number: ");
                        System.out.println(service.cancel(roll, prompt(scanner, "Cancellation request ID: "))); break;
                    }
                    case "7": {
                        String roll = prompt(scanner, "Student roll number: ");
                        RoomType type = RoomType.valueOf(prompt(scanner, "New room type (SINGLE/DOUBLE/SHARED): ").toUpperCase());
                        System.out.println(service.switchRoom(roll, type, prompt(scanner, "Switch request ID: "))); break;
                    }
                    case "8": {
                        long hours = Long.parseLong(prompt(scanner, "Hours to advance (1 or more): "));
                        service.advanceHours(hours);
                        System.out.println("Now: " + fmt(service.now)); break;
                    }
                    case "9": {
                        String roll = prompt(scanner, "Student roll number: ");
                        System.out.println(service.lookupRequest(roll, prompt(scanner, "Original booking request ID: "))); break;
                    }
                    case "10": service.printAudit(); break;
                    case "11": demo(); break;
                    case "12": HostelBookingGUI.launch(); break;
                    default: System.out.println("Invalid menu option.");
                }
            } catch (IllegalArgumentException exception) {
                System.out.println("Invalid input: " + exception.getMessage());
            }
        }
        scanner.close();
        System.out.println("Goodbye.");
    }

    public static void main(String[] args) {
        if (args.length == 1 && "--demo".equals(args[0])) demo();
        else if (args.length == 1 && "--self-test".equals(args[0])) selfTest();
        else if (args.length == 1 && "--gui".equals(args[0])) HostelBookingGUI.launch();
        else if (args.length == 1 && "--cli".equals(args[0])) menu();
        else if (args.length == 0) {
            if (!java.awt.GraphicsEnvironment.isHeadless()) {
                HostelBookingGUI.launch();
            } else {
                menu();
            }
        }
        else System.out.println("Usage: java HostelBookingSystem [--gui | --cli | --demo | --self-test]");
    }
}
