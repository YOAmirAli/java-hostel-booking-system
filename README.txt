SEN-311 SOFTWARE CONSTRUCTION - ASSIGNMENT 1
Aurora University Hostel Room Booking Portal
================================================

FILES
-----
Assignment1_Corrected_Documentation.docx   - Revised critique, 4-page redesign with flowchart,
                                    reflection, and SIX EMPTY screenshot spaces.
HostelBookingSystem.java           - Self-contained Java application & business logic service.
HostelBookingGUI.java              - Modern Java Swing Desktop GUI (Zero external dependencies).
README.txt                         - This file.
demo_output.txt                    - Text output from a verified demonstration;
                                    not a replacement for your own screenshots.

REQUIREMENTS
------------
Java JDK 8+ (the Java Development Kit must include javac and java).
No third-party Java libraries, external databases, or online access required.

HOW TO COMPILE / RUN (Windows CMD or PowerShell)
------------------------------------------------
1. Open a terminal in this folder.
2. Compile:
   javac HostelBookingSystem.java HostelBookingGUI.java

3. Launch Desktop GUI (Modern Visual Interface):
   java HostelBookingGUI
   (or: java HostelBookingSystem --gui)
   (or: java HostelBookingSystem)

4. Run Classic Console CLI Menu:
   java HostelBookingSystem --cli

5. Automatically demonstrate required scenarios:
   java HostelBookingSystem --demo

6. Run programmatic checks:
   java HostelBookingSystem --self-test

EXPECTED SELF-TEST RESULT
-------------------------
ALL 21 CHECKS PASSED (verify on your computer).
The self-test covers competing bookings, the one-active rule, request retries,
48-hour expiry, payment idempotency, switching, cancellation, audit events,
and eligible room access in a single-block prototype.

SCREENSHOTS TO INSERT INTO THE WORD REPORT
-------------------------------------------
The report ends with SIX deliberately EMPTY screenshot boxes. Do not submit
an empty evidence appendix if your teacher requires execution screenshots.
Run the program yourself and insert actual terminal captures. Suggested:

1. Last-room contention (demo Scenario 1).
2. Same request after a dropped response (demo Scenario 2).
3. Unpaid room automatically released after 48 hours (demo Scenario 3).
4. Paid room not released + repeated payment request (demo Scenario 4).
5. Cancellation and switching (demo Scenarios 5-6).
6. Time-stamped event audit and/or ALL 21 CHECKS PASSED (Scenario 7/self-test).

In Microsoft Word: click inside each blank blue-bordered space, select
Insert > Pictures > This Device, and size your own screenshot to fit.
Replace blank student name, roll number and section fields on the first page.

DESIGN NOTES / LIMITATIONS
--------------------------
- Interactive mode models Block A: 40 single, 30 double and 20 shared rooms.
- Each room is allocated to one booking in this teaching prototype. Shared
  bed assignment is a future policy/design extension, not implemented.
- Block C accessible allocation is described in the report; this one-block
  application refuses accessible requests to non-accessible Block A rooms.
- A clock begins at 2026-09-23 09:00. Advance it using option 8 to simulate
  48-hour deadlines; this does not wait in real time.
- A booking starts PENDING_DEPOSIT; it becomes CONFIRMED upon simulated
  deposit receipt, or EXPIRED if unpaid at the 48-hour deadline.
- A successful switch transfers a recorded payment, or preserves the
  ORIGINAL deposit deadline if still unpaid.
- All changes to availability and booking records happen together in
  synchronized Java service methods. Production would additionally need
  persistent DB transactions, unique constraints and payment verification.
- Request IDs are required for create/pay/switch/cancel. Retrying the same
  action with the same ID and payload does not perform the action twice.
- This program never charges real money and does not store passwords.
- Data and audit logs are kept in memory and reset when the program exits.

SUBMISSION REMINDERS
--------------------
The supplied case study has conflicting statements about group vs individual
work and the due date. Confirm these via the instructor or LMS.
The case study calls for original understanding and may have a viva.
Review and adapt the report in your own words, understand and test the code,
add your own output screenshots, and follow your instructor's integrity rules.
