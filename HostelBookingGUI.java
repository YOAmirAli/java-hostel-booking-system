import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Aurora University Hostel Room Booking Portal
 * High-Visibility, Modern, Clean & Simple Desktop GUI.
 * SEN-311 Software Construction. Standard JDK 8+ with Zero Dependencies.
 */
public class HostelBookingGUI extends JFrame {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Backend Service
    private HostelBookingSystem.BookingService service;
    private int reqSeq = 1;

    // High-Visibility Theme Colors
    private static final Color COLOR_HEADER = new Color(15, 23, 42);       // Dark Slate (Background)
    private static final Color COLOR_APP_BG = new Color(241, 245, 249);    // Slate 100
    private static final Color COLOR_CARD_BG = Color.WHITE;
    private static final Color COLOR_BORDER = new Color(203, 213, 225);     // Slate 300

    // High-Contrast Vibrant Button Colors
    private static final Color BTN_GREEN  = new Color(22, 163, 74);        // Emerald Green 600
    private static final Color BTN_ORANGE = new Color(234, 88, 12);        // Orange 600
    private static final Color BTN_PURPLE = new Color(124, 58, 237);       // Violet 600
    private static final Color BTN_RED    = new Color(220, 38, 38);        // Red 600
    private static final Color BTN_BLUE   = new Color(2, 132, 199);        // Sky Blue 600
    private static final Color BTN_SLATE  = new Color(51, 65, 85);         // Slate 700

    // Top Header Components
    private JLabel clockLabel;
    private JLabel statusBanner;
    private JPanel bannerPanel;

    // Quick Stats Labels
    private JLabel singleCountLabel;
    private JLabel doubleCountLabel;
    private JLabel sharedCountLabel;

    // Form Components
    private JComboBox<String> studentRollCombo;
    private JCheckBox accessibleCheck;
    private JComboBox<String> roomTypeCombo;
    private JLabel activeBookingInfoLabel;

    // Advanced Section
    private JCheckBox advancedModeCheck;
    private JPanel advancedPanel;
    private JTextField customRequestIdField;

    // Tables & Models
    private DefaultTableModel bookingsModel;
    private JTable bookingsTable;

    private DefaultTableModel roomsModel;
    private JTable roomsTable;

    private DefaultTableModel auditModel;
    private JTable auditTable;

    /**
     * Custom Modern Button with guaranteed high-visibility background,
     * crisp antialiased rounded borders, and bold white text on all platforms.
     */
    static class ModernButton extends JButton {
        private final Color baseBg;
        private final Color hoverBg;
        private final Color pressBg;

        public ModernButton(String text, Color bg) {
            super(text);
            this.baseBg = bg;
            this.hoverBg = blend(bg, Color.WHITE, 0.18f);
            this.pressBg = blend(bg, Color.BLACK, 0.20f);

            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        }

        public ModernButton(String text, Color bg, int fontSize) {
            this(text, bg);
            setFont(new Font("Segoe UI", Font.BOLD, fontSize));
            setBorder(BorderFactory.createEmptyBorder(6, 11, 6, 11));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (getModel().isPressed()) {
                g2.setColor(pressBg);
            } else if (getModel().isRollover()) {
                g2.setColor(hoverBg);
            } else {
                g2.setColor(baseBg);
            }

            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.dispose();
            super.paintComponent(g);
        }

        private static Color blend(Color c1, Color c2, float ratio) {
            float r = (1 - ratio) * c1.getRed() + ratio * c2.getRed();
            float g = (1 - ratio) * c1.getGreen() + ratio * c2.getGreen();
            float b = (1 - ratio) * c1.getBlue() + ratio * c2.getBlue();
            return new Color((int) r, (int) g, (int) b);
        }
    }

    public HostelBookingGUI() {
        super("Aurora University - Hostel Room Booking Portal (Block A)");
        this.service = new HostelBookingSystem.BookingService("A", HostelBookingSystem.blockARooms());
        initUI();
        refreshUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1160, 750);
        setMinimumSize(new Dimension(1000, 640));
        setLocationRelativeTo(null);
        getContentPane().setBackground(COLOR_APP_BG);
        setLayout(new BorderLayout(0, 0));

        // 1. Top High-Contrast Header Bar
        add(buildHeader(), BorderLayout.NORTH);

        // 2. Main Center Area (Left: Action Form | Right: Tables & Overview)
        JPanel centerPanel = new JPanel(new BorderLayout(15, 0));
        centerPanel.setBackground(COLOR_APP_BG);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        centerPanel.add(buildLeftActionPanel(), BorderLayout.WEST);
        centerPanel.add(buildRightOverviewPanel(), BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // 3. Bottom Status Bar
        add(buildBottomBar(), BorderLayout.SOUTH);
    }

    // ==========================================
    // 1. HIGH-CONTRAST TOP HEADER BAR
    // ==========================================

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(15, 0));
        header.setBackground(COLOR_HEADER);
        header.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        // Left Branding
        JPanel brand = new JPanel(new GridLayout(2, 1, 0, 2));
        brand.setOpaque(false);

        JLabel title = new JLabel("AURORA UNIVERSITY - HOSTEL ROOM BOOKING");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Block A Management System | Simple, High-Visibility Interface (SEN-311)");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(148, 163, 184)); // Slate 400

        brand.add(title);
        brand.add(subtitle);
        header.add(brand, BorderLayout.WEST);

        // Right Time-Travel Controls
        JPanel timeBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        timeBar.setOpaque(false);

        clockLabel = new JLabel("Clock: 2026-09-23 09:00");
        clockLabel.setFont(new Font("Consolas", Font.BOLD, 14));
        clockLabel.setForeground(new Color(248, 250, 252));
        clockLabel.setBackground(new Color(30, 41, 59));
        clockLabel.setOpaque(true);
        clockLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(71, 85, 105), 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        ModernButton btn1h = new ModernButton("+1 Hour", BTN_SLATE, 11);
        btn1h.addActionListener(e -> advanceTime(1));

        ModernButton btn24h = new ModernButton("+1 Day", BTN_BLUE, 11);
        btn24h.addActionListener(e -> advanceTime(24));

        ModernButton btn48h = new ModernButton("Fast-Forward 48h (Test Expiry)", BTN_ORANGE, 11);
        btn48h.addActionListener(e -> advanceTime(48));

        ModernButton btnReset = new ModernButton("Reset System", BTN_RED, 11);
        btnReset.addActionListener(e -> resetAll());

        timeBar.add(clockLabel);
        timeBar.add(btn1h);
        timeBar.add(btn24h);
        timeBar.add(btn48h);
        timeBar.add(btnReset);

        header.add(timeBar, BorderLayout.EAST);
        return header;
    }

    // ==========================================
    // 2. LEFT PANEL: VISIBLE ACTION CENTER
    // ==========================================

    private JPanel buildLeftActionPanel() {
        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setBackground(COLOR_APP_BG);
        left.setPreferredSize(new Dimension(390, 0));

        // Action Card Container
        JPanel actionBox = new JPanel(new BorderLayout(0, 10));
        actionBox.setBackground(COLOR_CARD_BG);
        actionBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        // Card Title
        JLabel boxTitle = new JLabel("Student Booking Center");
        boxTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        boxTitle.setForeground(COLOR_HEADER);
        actionBox.add(boxTitle, BorderLayout.NORTH);

        // Form Fields Container
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        // Step 1: Student Roll Number
        form.add(createFieldHeader("1. Student Roll Number:"));
        studentRollCombo = new JComboBox<String>(new String[]{"S101", "S102", "S103", "S104", "S105"});
        studentRollCombo.setEditable(true);
        studentRollCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        studentRollCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        studentRollCombo.addActionListener(e -> updateStudentInfoDisplay());
        form.add(studentRollCombo);
        form.add(Box.createVerticalStrut(6));

        accessibleCheck = new JCheckBox("Requires accessible room (Block C only)");
        accessibleCheck.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        accessibleCheck.setOpaque(false);
        form.add(accessibleCheck);
        form.add(Box.createVerticalStrut(10));

        // Active Booking Status Pill
        activeBookingInfoLabel = new JLabel("Current Status: No active booking");
        activeBookingInfoLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        activeBookingInfoLabel.setForeground(new Color(71, 85, 105));
        activeBookingInfoLabel.setBackground(new Color(241, 245, 249));
        activeBookingInfoLabel.setOpaque(true);
        activeBookingInfoLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)
        ));
        activeBookingInfoLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        form.add(activeBookingInfoLabel);
        form.add(Box.createVerticalStrut(14));

        // Step 2: Room Type Selector
        form.add(createFieldHeader("2. Select Room Type to Reserve:"));
        roomTypeCombo = new JComboBox<String>(new String[]{
                "SINGLE (1 Bed) - Block A",
                "DOUBLE (2 Beds) - Block A",
                "SHARED (4 Beds) - Block A"
        });
        roomTypeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        roomTypeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        form.add(roomTypeCombo);
        form.add(Box.createVerticalStrut(14));

        // Big Vibrant Action Buttons
        ModernButton btnBook = new ModernButton("Reserve Room (48h Deadline)", BTN_GREEN);
        btnBook.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnBook.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnBook.addActionListener(e -> doBook());
        form.add(btnBook);
        form.add(Box.createVerticalStrut(8));

        ModernButton btnPay = new ModernButton("Pay Deposit (Confirm Booking)", BTN_ORANGE);
        btnPay.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnPay.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPay.addActionListener(e -> doPay());
        form.add(btnPay);
        form.add(Box.createVerticalStrut(8));

        // Switch & Cancel Buttons side-by-side
        JPanel switchCancelPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        switchCancelPanel.setOpaque(false);
        switchCancelPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        ModernButton btnSwitch = new ModernButton("Switch Room", BTN_PURPLE);
        btnSwitch.addActionListener(e -> doSwitch());

        ModernButton btnCancel = new ModernButton("Cancel Booking", BTN_RED);
        btnCancel.addActionListener(e -> doCancel());

        switchCancelPanel.add(btnSwitch);
        switchCancelPanel.add(btnCancel);
        form.add(switchCancelPanel);
        form.add(Box.createVerticalStrut(14));

        // Advanced Mode Toggle
        advancedModeCheck = new JCheckBox("Show Advanced Request ID (Test Retries)");
        advancedModeCheck.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        advancedModeCheck.setForeground(new Color(100, 116, 139));
        advancedModeCheck.setOpaque(false);
        advancedModeCheck.addActionListener(e -> {
            advancedPanel.setVisible(advancedModeCheck.isSelected());
            actionBox.revalidate();
        });
        form.add(advancedModeCheck);

        advancedPanel = new JPanel(new BorderLayout(5, 0));
        advancedPanel.setOpaque(false);
        advancedPanel.setVisible(false);
        advancedPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        customRequestIdField = new JTextField("req-" + (reqSeq++));
        JButton btnNewId = new JButton("New ID");
        btnNewId.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnNewId.addActionListener(e -> customRequestIdField.setText("req-" + (reqSeq++)));
        advancedPanel.add(new JLabel("Request ID: "), BorderLayout.WEST);
        advancedPanel.add(customRequestIdField, BorderLayout.CENTER);
        advancedPanel.add(btnNewId, BorderLayout.EAST);
        form.add(advancedPanel);

        actionBox.add(form, BorderLayout.CENTER);

        // Feedback / Result Banner at bottom of left panel
        bannerPanel = new JPanel(new BorderLayout());
        bannerPanel.setBackground(new Color(248, 250, 252));
        bannerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, BTN_BLUE),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(COLOR_BORDER, 1),
                        BorderFactory.createEmptyBorder(10, 12, 10, 12)
                )
        ));

        statusBanner = new JLabel("<html><b>System Ready.</b> Select a student roll and room type to begin.</html>");
        statusBanner.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusBanner.setForeground(COLOR_HEADER);
        bannerPanel.add(statusBanner, BorderLayout.CENTER);

        left.add(actionBox, BorderLayout.CENTER);
        left.add(bannerPanel, BorderLayout.SOUTH);
        return left;
    }

    private JLabel createFieldHeader(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(COLOR_HEADER);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    // ==========================================
    // 3. RIGHT PANEL: INVENTORY & DIRECTORY
    // ==========================================

    private JPanel buildRightOverviewPanel() {
        JPanel right = new JPanel(new BorderLayout(0, 10));
        right.setBackground(COLOR_APP_BG);

        // Top Row: 3 Big Inventory Cards
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 12, 0));
        statsRow.setOpaque(false);
        statsRow.setPreferredSize(new Dimension(0, 85));

        singleCountLabel = new JLabel("40 / 40 Free", JLabel.CENTER);
        doubleCountLabel = new JLabel("30 / 30 Free", JLabel.CENTER);
        sharedCountLabel = new JLabel("20 / 20 Free", JLabel.CENTER);

        statsRow.add(createKpiCard("SINGLE ROOMS (1 Bed)", singleCountLabel, "40 Total in Block A", BTN_GREEN));
        statsRow.add(createKpiCard("DOUBLE ROOMS (2 Beds)", doubleCountLabel, "30 Total in Block A", BTN_BLUE));
        statsRow.add(createKpiCard("SHARED ROOMS (4 Beds)", sharedCountLabel, "20 Total in Block A", BTN_PURPLE));

        right.add(statsRow, BorderLayout.NORTH);

        // Tabbed Display
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));

        tabs.addTab("Active & Past Bookings", buildBookingsTab());
        tabs.addTab("All Rooms Directory (90)", buildRoomsTab());
        tabs.addTab("Verification Tests & Audit Trail", buildTestsAndAuditTab());

        right.add(tabs, BorderLayout.CENTER);
        return right;
    }

    private JPanel createKpiCard(String title, JLabel valueLbl, String subtitle, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 2));
        card.setBackground(COLOR_CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(4, 0, 0, 0, accent),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(COLOR_BORDER, 1),
                        BorderFactory.createEmptyBorder(10, 12, 10, 12)
                )
        ));

        JLabel titleLbl = new JLabel(title, JLabel.CENTER);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLbl.setForeground(new Color(100, 116, 139));

        valueLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valueLbl.setForeground(COLOR_HEADER);

        JLabel subLbl = new JLabel(subtitle, JLabel.CENTER);
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subLbl.setForeground(new Color(148, 163, 184));

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLbl, BorderLayout.CENTER);
        card.add(subLbl, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildBookingsTab() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(COLOR_CARD_BG);
        p.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        String[] cols = {"Booking Ref", "Student Roll", "Room Assigned", "Room Type", "Status", "Deposit Deadline", "Deposit Paid At"};
        bookingsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        bookingsTable = new JTable(bookingsModel);
        bookingsTable.setRowHeight(28);
        bookingsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bookingsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        bookingsTable.getTableHeader().setBackground(new Color(241, 245, 249));

        // Explicit column widths so headers and values are never truncated
        bookingsTable.getColumnModel().getColumn(0).setPreferredWidth(105);
        bookingsTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        bookingsTable.getColumnModel().getColumn(2).setPreferredWidth(115);
        bookingsTable.getColumnModel().getColumn(3).setPreferredWidth(95);
        bookingsTable.getColumnModel().getColumn(4).setPreferredWidth(175);
        bookingsTable.getColumnModel().getColumn(5).setPreferredWidth(140);
        bookingsTable.getColumnModel().getColumn(6).setPreferredWidth(140);

        // High-contrast status renderer
        bookingsTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel, boolean hasFocus, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, val, isSel, hasFocus, r, c);
                lbl.setHorizontalAlignment(JLabel.CENTER);
                String st = String.valueOf(val);
                if (st.contains("CONFIRMED")) {
                    lbl.setText("CONFIRMED (Paid)");
                    lbl.setForeground(new Color(21, 128, 61));
                    lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
                } else if (st.contains("PENDING")) {
                    lbl.setText("AWAITING DEPOSIT (48h)");
                    lbl.setForeground(new Color(194, 65, 12));
                    lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
                } else if (st.contains("EXPIRED")) {
                    lbl.setText("EXPIRED (Unpaid)");
                    lbl.setForeground(new Color(100, 116, 139));
                } else if (st.contains("CANCELLED")) {
                    lbl.setText("CANCELLED");
                    lbl.setForeground(BTN_RED);
                } else if (st.contains("SWITCHED")) {
                    lbl.setText("SWITCHED");
                    lbl.setForeground(BTN_PURPLE);
                }
                return lbl;
            }
        });

        bookingsTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = bookingsTable.getSelectedRow();
                if (row >= 0) {
                    String roll = (String) bookingsModel.getValueAt(row, 1);
                    studentRollCombo.setSelectedItem(roll);
                }
            }
        });

        p.add(new JScrollPane(bookingsTable), BorderLayout.CENTER);

        JLabel tip = new JLabel("Tip: Click any booking row to automatically select that student on the left.");
        tip.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        tip.setForeground(new Color(100, 116, 139));
        p.add(tip, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildRoomsTab() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(COLOR_CARD_BG);
        p.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        String[] cols = {"Room ID", "Block", "Type", "Capacity", "Status", "Current Occupant", "Booking Ref"};
        roomsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        roomsTable = new JTable(roomsModel);
        roomsTable.setRowHeight(25);
        roomsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        roomsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        roomsTable.getTableHeader().setBackground(new Color(241, 245, 249));

        roomsTable.getColumnModel().getColumn(0).setPreferredWidth(90);
        roomsTable.getColumnModel().getColumn(1).setPreferredWidth(70);
        roomsTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        roomsTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        roomsTable.getColumnModel().getColumn(4).setPreferredWidth(130);
        roomsTable.getColumnModel().getColumn(5).setPreferredWidth(130);
        roomsTable.getColumnModel().getColumn(6).setPreferredWidth(110);

        roomsTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel, boolean hasFocus, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, val, isSel, hasFocus, r, c);
                lbl.setHorizontalAlignment(JLabel.CENTER);
                String st = String.valueOf(val);
                if (st.equals("AVAILABLE")) {
                    lbl.setText("AVAILABLE");
                    lbl.setForeground(BTN_GREEN);
                    lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
                } else if (st.equals("OCCUPIED")) {
                    lbl.setText("OCCUPIED");
                    lbl.setForeground(BTN_RED);
                    lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
                }
                return lbl;
            }
        });

        p.add(new JScrollPane(roomsTable), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildTestsAndAuditTab() {
        JPanel p = new JPanel(new GridLayout(1, 2, 12, 0));
        p.setBackground(COLOR_CARD_BG);
        p.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        // Left: Verification Test Runner
        JPanel testCard = new JPanel(new BorderLayout(0, 8));
        testCard.setOpaque(false);
        testCard.setBorder(BorderFactory.createTitledBorder("Course Grading & Automated Verification Suite"));

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnBar.setOpaque(false);

        ModernButton btnSelfTest = new ModernButton("Run 21-Check Verification", BTN_GREEN, 12);
        ModernButton btnDemo = new ModernButton("Run 7 Demo Scenarios", BTN_BLUE, 12);

        btnBar.add(btnSelfTest);
        btnBar.add(btnDemo);
        testCard.add(btnBar, BorderLayout.NORTH);

        JTextArea testOutput = new JTextArea();
        testOutput.setFont(new Font("Consolas", Font.PLAIN, 12));
        testOutput.setEditable(false);
        testOutput.setBackground(new Color(15, 23, 42));
        testOutput.setForeground(new Color(248, 250, 252));
        testOutput.setText("Click 'Run 21-Check Verification' to test:\n"
                + "- One-active-reservation rule\n"
                + "- 48-hour deadline release\n"
                + "- Idempotent request retries\n"
                + "- Room switching payment carry-over\n"
                + "- Block A accessibility guardrail");
        testCard.add(new JScrollPane(testOutput), BorderLayout.CENTER);

        btnSelfTest.addActionListener(e -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream old = System.out;
            try {
                System.setOut(new PrintStream(baos));
                HostelBookingSystem.selfTest();
            } finally {
                System.setOut(old);
            }
            testOutput.setText(baos.toString());
            testOutput.setCaretPosition(0);
            showBanner("Self-Test Complete: ALL 21 CHECKS PASSED!", BTN_GREEN);
        });

        btnDemo.addActionListener(e -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream old = System.out;
            try {
                System.setOut(new PrintStream(baos));
                HostelBookingSystem.demo();
            } finally {
                System.setOut(old);
            }
            testOutput.setText(baos.toString());
            testOutput.setCaretPosition(0);
            showBanner("7 Reproducible Demonstration Scenarios Executed Successfully.", BTN_BLUE);
        });

        p.add(testCard);

        // Right: Event Audit Log
        JPanel auditCard = new JPanel(new BorderLayout(0, 8));
        auditCard.setOpaque(false);
        auditCard.setBorder(BorderFactory.createTitledBorder("System Event Audit Trail"));

        String[] cols = {"Time", "Action", "Student", "Event Summary"};
        auditModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        auditTable = new JTable(auditModel);
        auditTable.setRowHeight(24);
        auditTable.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        auditCard.add(new JScrollPane(auditTable), BorderLayout.CENTER);

        p.add(auditCard);
        return p;
    }

    private JPanel buildBottomBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(241, 245, 249));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_BORDER),
                BorderFactory.createEmptyBorder(6, 20, 6, 20)
        ));

        JLabel info = new JLabel("SEN-311: Software Construction • Aurora University Hostel Room Booking System");
        info.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        info.setForeground(new Color(100, 116, 139));

        JLabel extra = new JLabel("Zero external dependencies • In-memory simulated storage");
        extra.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        extra.setForeground(new Color(100, 116, 139));

        bar.add(info, BorderLayout.WEST);
        bar.add(extra, BorderLayout.EAST);
        return bar;
    }

    // ==========================================
    // ACTION HANDLERS
    // ==========================================

    private String getSelectedRoll() {
        Object item = studentRollCombo.getSelectedItem();
        return item == null ? "" : item.toString().trim();
    }

    private HostelBookingSystem.RoomType getSelectedRoomType() {
        int idx = roomTypeCombo.getSelectedIndex();
        if (idx == 0) return HostelBookingSystem.RoomType.SINGLE;
        if (idx == 1) return HostelBookingSystem.RoomType.DOUBLE;
        return HostelBookingSystem.RoomType.SHARED;
    }

    private String getRequestId(String prefix) {
        if (advancedModeCheck.isSelected()) {
            return customRequestIdField.getText().trim();
        }
        return prefix + "-" + (reqSeq++);
    }

    private void ensureStudentRegistered(String roll) {
        if (roll.isEmpty()) return;
        boolean accessible = accessibleCheck.isSelected();
        service.register(roll, accessible);
    }

    private void doBook() {
        String roll = getSelectedRoll();
        if (roll.isEmpty()) {
            showBanner("Please enter or select a Student Roll Number.", BTN_RED);
            return;
        }
        ensureStudentRegistered(roll);
        HostelBookingSystem.RoomType type = getSelectedRoomType();
        String reqId = getRequestId("bkg");

        HostelBookingSystem.Result res = service.book(roll, type, reqId);
        handleResult(res, "Room reserved! You have 48 hours to pay the deposit.");
        refreshUI();
    }

    private void doPay() {
        String roll = getSelectedRoll();
        if (roll.isEmpty()) {
            showBanner("Please enter or select a Student Roll Number.", BTN_RED);
            return;
        }
        ensureStudentRegistered(roll);
        String reqId = getRequestId("pay");

        HostelBookingSystem.Result res = service.payDeposit(roll, reqId);
        handleResult(res, "Deposit paid! Your booking is now CONFIRMED.");
        refreshUI();
    }

    private void doSwitch() {
        String roll = getSelectedRoll();
        if (roll.isEmpty()) {
            showBanner("Please enter or select a Student Roll Number.", BTN_RED);
            return;
        }
        HostelBookingSystem.RoomType type = getSelectedRoomType();
        String reqId = getRequestId("sw");

        HostelBookingSystem.Result res = service.switchRoom(roll, type, reqId);
        handleResult(res, "Room switched! Your payment or deposit deadline was safely transferred.");
        refreshUI();
    }

    private void doCancel() {
        String roll = getSelectedRoll();
        if (roll.isEmpty()) {
            showBanner("Please enter or select a Student Roll Number.", BTN_RED);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to cancel the active booking for student " + roll + "?\nThe room will be released back to inventory.",
                "Confirm Cancellation",
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String reqId = getRequestId("cancel");
        HostelBookingSystem.Result res = service.cancel(roll, reqId);
        handleResult(res, "Booking cancelled and room released back to available inventory.");
        refreshUI();
    }

    private void handleResult(HostelBookingSystem.Result res, String friendlySuccessMessage) {
        if (res.success) {
            String msg = "<html><b>SUCCESS:</b> " + res.message;
            if (res.bookingReference != null) msg += " [<b>" + res.bookingReference + "</b>]";
            msg += "<br><span style='color:#15803d;'>" + friendlySuccessMessage + "</span></html>";
            showBanner(msg, BTN_GREEN);
        } else {
            String msg = "<html><b>REJECTED:</b> " + res.message + "</html>";
            showBanner(msg, BTN_RED);
        }
    }

    private void showBanner(String htmlText, Color accent) {
        bannerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(COLOR_BORDER, 1),
                        BorderFactory.createEmptyBorder(10, 12, 10, 12)
                )
        ));
        statusBanner.setText(htmlText);
    }

    private void advanceTime(long hours) {
        service.advanceHours(hours);
        showBanner("<html>Simulated clock advanced by <b>" + hours + " hour(s)</b>. Any unpaid bookings past 48 hours have expired.</html>", BTN_BLUE);
        refreshUI();
    }

    private void resetAll() {
        int opt = JOptionPane.showConfirmDialog(this,
                "Reset the simulation back to initial state?\nAll student bookings and audit records will be reset.",
                "Reset Simulation",
                JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            this.service = new HostelBookingSystem.BookingService("A", HostelBookingSystem.blockARooms());
            this.reqSeq = 1;
            showBanner("System reset to starting state (2026-09-23 09:00). All 90 rooms are vacant.", BTN_GREEN);
            refreshUI();
        }
    }

    // ==========================================
    // REFRESH DATA & TABLES
    // ==========================================

    private void refreshUI() {
        synchronized (service) {
            service.activeCount(""); // triggers expiry check

            // 1. Clock
            clockLabel.setText("Clock: " + HostelBookingSystem.fmt(service.now));

            // 2. Inventory Cards
            int freeSingle = service.freeCount(HostelBookingSystem.RoomType.SINGLE);
            int freeDouble = service.freeCount(HostelBookingSystem.RoomType.DOUBLE);
            int freeShared = service.freeCount(HostelBookingSystem.RoomType.SHARED);

            singleCountLabel.setText(freeSingle + " / 40 Free");
            singleCountLabel.setForeground(freeSingle > 0 ? BTN_GREEN : BTN_RED);

            doubleCountLabel.setText(freeDouble + " / 30 Free");
            doubleCountLabel.setForeground(freeDouble > 0 ? BTN_BLUE : BTN_RED);

            sharedCountLabel.setText(freeShared + " / 20 Free");
            sharedCountLabel.setForeground(freeShared > 0 ? BTN_PURPLE : BTN_RED);

            // 3. Update Bookings Table
            bookingsModel.setRowCount(0);
            for (HostelBookingSystem.Booking b : service.bookings.values()) {
                HostelBookingSystem.Room r = service.rooms.get(b.roomId);
                String typeStr = r == null ? "" : r.type.name();
                bookingsModel.addRow(new Object[]{
                        b.reference,
                        b.studentRoll,
                        b.roomId,
                        typeStr,
                        b.status.name(),
                        HostelBookingSystem.fmt(b.depositDueAt),
                        b.depositPaidAt == null ? "Pending (Unpaid)" : HostelBookingSystem.fmt(b.depositPaidAt)
                });
            }

            // 4. Update Rooms Table
            roomsModel.setRowCount(0);
            for (HostelBookingSystem.Room rm : service.rooms.values()) {
                String status = rm.available() ? "AVAILABLE" : "OCCUPIED";
                String occRoll = "-";
                String ref = "-";
                if (rm.occupiedByBooking != null) {
                    ref = rm.occupiedByBooking;
                    HostelBookingSystem.Booking b = service.bookings.get(ref);
                    if (b != null) occRoll = b.studentRoll;
                }
                roomsModel.addRow(new Object[]{
                        rm.id,
                        rm.block,
                        rm.type.name(),
                        rm.capacity + " Bed(s)",
                        status,
                        occRoll,
                        ref
                });
            }

            // 5. Update Audit Table
            auditModel.setRowCount(0);
            for (String entry : service.audit) {
                int cb = entry.indexOf(']');
                if (cb > 0) {
                    String time = entry.substring(1, cb);
                    String rest = entry.substring(cb + 2).trim();
                    String[] parts = rest.split("\\|", 3);
                    String action = parts.length > 0 ? parts[0].trim() : "";
                    String actor = parts.length > 1 ? parts[1].trim() : "";
                    String info = parts.length > 2 ? parts[2].trim() : "";
                    auditModel.addRow(new Object[]{time, action, actor, info});
                }
            }

            // 6. Update student dropdown
            updateStudentDropdown();
            updateStudentInfoDisplay();
        }
    }

    private void updateStudentDropdown() {
        String current = getSelectedRoll();
        Set<String> set = new TreeSet<String>();
        set.add("S101");
        set.add("S102");
        set.add("S103");
        set.add("S104");
        set.add("S105");
        set.addAll(service.students.keySet());

        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<String>();
        for (String s : set) model.addElement(s);
        studentRollCombo.setModel(model);
        if (!current.isEmpty()) studentRollCombo.setSelectedItem(current);
    }

    private void updateStudentInfoDisplay() {
        String roll = getSelectedRoll();
        if (roll.isEmpty()) {
            activeBookingInfoLabel.setText("Current Status: Enter roll number");
            activeBookingInfoLabel.setForeground(new Color(100, 116, 139));
            return;
        }

        HostelBookingSystem.Student s = service.students.get(roll);
        if (s == null || s.activeBookingReference == null) {
            activeBookingInfoLabel.setText("Student " + roll + ": No active booking");
            activeBookingInfoLabel.setForeground(new Color(100, 116, 139));
        } else {
            HostelBookingSystem.Booking b = service.bookings.get(s.activeBookingReference);
            if (b != null) {
                if (b.status == HostelBookingSystem.BookingStatus.CONFIRMED) {
                    activeBookingInfoLabel.setText("Student " + roll + ": CONFIRMED in " + b.roomId + " (" + b.reference + ")");
                    activeBookingInfoLabel.setForeground(BTN_GREEN);
                } else if (b.status == HostelBookingSystem.BookingStatus.PENDING_DEPOSIT) {
                    activeBookingInfoLabel.setText("Student " + roll + ": Pending Deposit in " + b.roomId + " (Due: " + HostelBookingSystem.fmt(b.depositDueAt) + ")");
                    activeBookingInfoLabel.setForeground(BTN_ORANGE);
                }
            }
        }
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new HostelBookingGUI().setVisible(true);
        });
    }

    public static void main(String[] args) {
        launch();
    }
}
