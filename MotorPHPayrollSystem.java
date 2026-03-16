import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Scanner;

public class MyPH {

    // =========================================================
    // PROGRAM CONSTANTS
    // =========================================================
    public static final int WORK_START = 480;   // 8:00 AM
    public static final int GRACE_LIMIT = 485;  // 8:05 AM
    public static final int WORK_END = 1020;    // 5:00 PM

    public static final String EMPLOYEE_FILE = "EmployeeDetails.csv";
    public static final String ATTENDANCE_FILE = "Attendance.csv";

    // =========================================================
    // CLEAN FIELD
    // Removes quotes, BOM, hidden spaces, tabs, and trims text.
    // =========================================================
    public static String cleanField(String s) {
        if (s == null) return "";
        return s.replace("\"", "")
                .replace("\uFEFF", "")
                .replace("\u200B", "")
                .replace("\u00A0", "")
                .replace("\r", "")
                .replace("\t", "")
                .trim();
    }

    // =========================================================
    // PARSE CSV LINE
    // Handles commas inside quoted fields.
    // =========================================================
    public static String[] parseCSVLine(String line) {
        ArrayList<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        fields.add(current.toString().trim());
        return fields.toArray(new String[0]);
    }

    // =========================================================
    // SAFE PARSE DOUBLE
    // Converts CSV numeric field safely.
    // =========================================================
    public static double safeParseDouble(String s) {
        try {
            return Double.parseDouble(cleanField(s).replace(",", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    // =========================================================
    // SAFE PARSE INT
    // Converts menu input safely.
    // =========================================================
    public static int safeParseInt(String s) {
        try {
            return Integer.parseInt(cleanField(s));
        } catch (Exception e) {
            return -1;
        }
    }

    // =========================================================
    // FIND EMPLOYEE
    // Searches employee list by employee number.
    // =========================================================
    public static String[] findEmployee(ArrayList<String[]> employees, String empNum) {
        String target = cleanField(empNum);

        for (int i = 1; i < employees.size(); i++) { // skip header
            String[] emp = employees.get(i);
            if (emp.length == 0) continue;

            String currentEmpNum = cleanField(emp[0]);
            if (currentEmpNum.equals(target)) {
                return emp;
            }
        }
        return null;
    }

    // =========================================================
    // TIME TO MINUTES
    // Converts time string to total minutes.
    // Examples:
    // 8:30 AM -> 510
    // 5:00 PM -> 1020
    // =========================================================
    public static int convertToMinutes(String time) {
        if (time == null) return -1;

        time = cleanField(time);
        if (time.isEmpty()) return -1;

        try {
            if (time.contains("AM") || time.contains("PM")) {
                String[] parts = time.split(" ");
                if (parts.length < 2) return -1;

                String[] hm = parts[0].split(":");
                if (hm.length < 2) return -1;

                int hour = Integer.parseInt(hm[0].trim());
                int minute = Integer.parseInt(hm[1].trim());
                String ampm = parts[1].trim();

                if (ampm.equalsIgnoreCase("PM") && hour != 12) hour += 12;
                if (ampm.equalsIgnoreCase("AM") && hour == 12) hour = 0;

                return hour * 60 + minute;
            }

            String[] hm = time.split(":");
            if (hm.length < 2) return -1;

            int hour = Integer.parseInt(hm[0].trim());
            int minute = Integer.parseInt(hm[1].trim());

            return hour * 60 + minute;

        } catch (Exception e) {
            return -1;
        }
    }

    // =========================================================
    // COMPUTE HOURS WORKED
    //
    // Rules followed:
    // 1. Count only from 8:00 AM to 5:00 PM
    // 2. Do not include extra hours beyond 5:00 PM
    // 3. If login is 8:05 AM or earlier, treat it as 8:00 AM
    // 4. Always deduct 1 hour lunch
    // =========================================================
    public static double computeHoursWorked(String logIn, String logOut) {
        int inMin = convertToMinutes(logIn);
        int outMin = convertToMinutes(logOut);

        if (inMin == -1 || outMin == -1) return 0;

        // Grace period rule
        if (inMin <= GRACE_LIMIT) {
            inMin = WORK_START;
        }

        // Count only within official work hours
        if (inMin < WORK_START) inMin = WORK_START;
        if (outMin > WORK_END) outMin = WORK_END;

        // Invalid interval
        if (outMin <= inMin) return 0;

        int workedMinutes = outMin - inMin;

        // Always deduct 1 hour lunch
        workedMinutes -= 60;

        if (workedMinutes < 0) return 0;

        return workedMinutes / 60.0;
    }

    // =========================================================
    // CHECK IF DATE IS INSIDE CUTOFF
    // Expected format: MM/DD/YYYY
    // =========================================================
    public static boolean isInCutoff(String date, int month, int startDay, int endDay) {
        try {
            String[] parts = cleanField(date).split("/");
            if (parts.length < 3) return false;

            int m = Integer.parseInt(parts[0].trim());
            int d = Integer.parseInt(parts[1].trim());

            return m == month && d >= startDay && d <= endDay;
        } catch (Exception e) {
            return false;
        }
    }

    // =========================================================
    // COMPUTE TOTAL HOURS FOR ONE EMPLOYEE IN ONE CUTOFF
    // =========================================================
    public static double computeCutoffHours(ArrayList<String[]> attendance,
                                            String empNum,
                                            int month,
                                            int startDay,
                                            int endDay) {
        double totalHours = 0;
        String targetEmpNum = cleanField(empNum);

        for (int i = 1; i < attendance.size(); i++) { // skip header
            String[] att = attendance.get(i);

            // Expected columns:
            // [0] employee number
            // [3] date
            // [4] log in
            // [5] log out
            if (att.length < 6) continue;

            String currentEmpNum = cleanField(att[0]);
            if (!currentEmpNum.equals(targetEmpNum)) continue;

            String date = cleanField(att[3]);
            if (!isInCutoff(date, month, startDay, endDay)) continue;

            String logIn = cleanField(att[4]);
            String logOut = cleanField(att[5]);

            if (logIn.isEmpty() || logOut.isEmpty()) continue;

            totalHours += computeHoursWorked(logIn, logOut);
        }

        return totalHours;
    }

    // =========================================================
    // GOVERNMENT DEDUCTIONS
    // Based on combined monthly gross salary.
    // =========================================================
    public static double computeSSS(double monthlyGross) {
        if (monthlyGross < 4250)       return 180.00;
        else if (monthlyGross < 4750)  return 202.50;
        else if (monthlyGross < 5250)  return 225.00;
        else if (monthlyGross < 5750)  return 247.50;
        else if (monthlyGross < 6250)  return 270.00;
        else if (monthlyGross < 6750)  return 292.50;
        else if (monthlyGross < 7250)  return 315.00;
        else if (monthlyGross < 7750)  return 337.50;
        else if (monthlyGross < 8250)  return 360.00;
        else if (monthlyGross < 8750)  return 382.50;
        else if (monthlyGross < 9250)  return 405.00;
        else if (monthlyGross < 9750)  return 427.50;
        else if (monthlyGross < 10250) return 450.00;
        else if (monthlyGross < 10750) return 472.50;
        else if (monthlyGross < 11250) return 495.00;
        else if (monthlyGross < 11750) return 517.50;
        else if (monthlyGross < 12250) return 540.00;
        else if (monthlyGross < 12750) return 562.50;
        else if (monthlyGross < 13250) return 585.00;
        else if (monthlyGross < 13750) return 607.50;
        else if (monthlyGross < 14250) return 630.00;
        else if (monthlyGross < 14750) return 652.50;
        else if (monthlyGross < 15250) return 675.00;
        else if (monthlyGross < 15750) return 697.50;
        else if (monthlyGross < 16250) return 720.00;
        else if (monthlyGross < 16750) return 742.50;
        else if (monthlyGross < 17250) return 765.00;
        else if (monthlyGross < 17750) return 787.50;
        else if (monthlyGross < 18250) return 810.00;
        else if (monthlyGross < 18750) return 832.50;
        else if (monthlyGross < 19250) return 855.00;
        else if (monthlyGross < 19750) return 877.50;
        else if (monthlyGross < 20250) return 900.00;
        else if (monthlyGross < 20750) return 922.50;
        else if (monthlyGross < 21250) return 945.00;
        else if (monthlyGross < 21750) return 967.50;
        else if (monthlyGross < 22250) return 990.00;
        else if (monthlyGross < 22750) return 1012.50;
        else if (monthlyGross < 23250) return 1035.00;
        else if (monthlyGross < 23750) return 1057.50;
        else if (monthlyGross < 24250) return 1080.00;
        else if (monthlyGross < 24750) return 1102.50;
        else                           return 1125.00;
    }

    public static double computePhilHealth(double monthlyGross) {
        if (monthlyGross < 10000) return 250.00;
        else if (monthlyGross > 100000) return 2500.00;
        else return monthlyGross * 0.025;
    }

    public static double computePagIBIG(double monthlyGross) {
        if (monthlyGross <= 1500) return monthlyGross * 0.01;
        else return 100.00;
    }

    public static double computeWithholdingTax(double monthlyGross,
                                               double sss,
                                               double philhealth,
                                               double pagibig) {
        double taxableIncome = monthlyGross - sss - philhealth - pagibig;

        if (taxableIncome <= 20833)       return 0;
        else if (taxableIncome <= 33332)  return (taxableIncome - 20833) * 0.20;
        else if (taxableIncome <= 66666)  return 2500 + (taxableIncome - 33333) * 0.25;
        else if (taxableIncome <= 166666) return 10833 + (taxableIncome - 66667) * 0.30;
        else if (taxableIncome <= 666666) return 40833.33 + (taxableIncome - 166667) * 0.32;
        else                              return 200833.33 + (taxableIncome - 666667) * 0.35;
    }

    // =========================================================
    // PRINT FIRST CUTOFF
    // =========================================================
    public static void printFirstCutoff(String monthName,
                                        int startDay,
                                        int endDay,
                                        double hours,
                                        double gross,
                                        double net) {
        System.out.println("Cutoff Date: " + monthName + " " + startDay + " to " + monthName + " " + endDay);
        System.out.println("Total Hours Worked: " + hours);
        System.out.println("Gross Salary: " + gross);
        System.out.println("Net Salary: " + net);
        System.out.println();
    }

    // =========================================================
    // PRINT SECOND CUTOFF
    // =========================================================
    public static void printSecondCutoff(String monthName,
                                         int startDay,
                                         int endDay,
                                         double hours,
                                         double gross,
                                         double sss,
                                         double philhealth,
                                         double pagibig,
                                         double tax,
                                         double totalDeductions,
                                         double net) {
        System.out.println("Cutoff Date: " + monthName + " " + startDay + " to " + monthName + " " + endDay);
        System.out.println("Total Hours Worked: " + hours);
        System.out.println("Gross Salary: " + gross);
        System.out.println("SSS: " + sss);
        System.out.println("PhilHealth: " + philhealth);
        System.out.println("Pag-IBIG: " + pagibig);
        System.out.println("Tax: " + tax);
        System.out.println("Total Deductions: " + totalDeductions);
        System.out.println("Net Salary: " + net);
        System.out.println();
    }

    // =========================================================
    // DISPLAY PAYROLL FOR ONE EMPLOYEE
    // Shows all cutoffs from June to December.
    // =========================================================
    public static void displayPayroll(String[] emp, ArrayList<String[]> attendance) {

        // Adjust indexes only if your CSV layout is different.
        String empNum = cleanField(emp[0]);
        String fullName = cleanField(emp[2]) + " " + cleanField(emp[1]);
        String birthday = cleanField(emp[3]);
        double hourlyRate = safeParseDouble(emp[18]);

        System.out.println("Employee Information");
        System.out.println("Employee #: " + empNum);
        System.out.println("Employee Name: " + fullName);
        System.out.println("Birthday: " + birthday);
        System.out.println();

        int[] months =    {6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12};
        int[] startDays = {1, 16, 1, 16, 1, 16, 1, 16, 1, 16, 1, 16, 1, 16};
        int[] endDays =   {15, 30, 15, 31, 15, 31, 15, 30, 15, 31, 15, 30, 15, 31};

        String[] monthNames = {
                "", "", "", "", "", "", "June", "July", "August",
                "September", "October", "November", "December"
        };

        int totalPairs = months.length / 2;

        for (int p = 0; p < totalPairs; p++) {
            int idx1 = p * 2;
            int idx2 = p * 2 + 1;
            int month = months[idx1];

            double hours1 = computeCutoffHours(attendance, empNum, month, startDays[idx1], endDays[idx1]);
            double hours2 = computeCutoffHours(attendance, empNum, month, startDays[idx2], endDays[idx2]);

            double gross1 = hours1 * hourlyRate;
            double gross2 = hours2 * hourlyRate;

            // Combine first and second cutoff before deductions
            double monthlyGross = gross1 + gross2;

            double sss = computeSSS(monthlyGross);
            double philhealth = computePhilHealth(monthlyGross);
            double pagibig = computePagIBIG(monthlyGross);
            double tax = computeWithholdingTax(monthlyGross, sss, philhealth, pagibig);
            double totalDeductions = sss + philhealth + pagibig + tax;

            // First cutoff has no deductions
            double net1 = gross1;

            // Second cutoff includes all deductions
            double net2 = gross2 - totalDeductions;

            printFirstCutoff(monthNames[month], startDays[idx1], endDays[idx1], hours1, gross1, net1);
            printSecondCutoff(monthNames[month], startDays[idx2], endDays[idx2], hours2, gross2,
                    sss, philhealth, pagibig, tax, totalDeductions, net2);
        }
    }

    // =========================================================
    // LOAD CSV FILE
    // =========================================================
    public static boolean loadCSV(String fileName, ArrayList<String[]> targetList) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;

            while ((line = br.readLine()) != null) {
                if (targetList.isEmpty() && line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                targetList.add(parseCSVLine(line));
            }

            return true;

        } catch (Exception e) {
            System.out.println("Error reading file: " + fileName);
            return false;
        }
    }

    // =========================================================
    // MAIN PROGRAM
    // =========================================================
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        ArrayList<String[]> employees = new ArrayList<>();
        ArrayList<String[]> attendance = new ArrayList<>();

        // Load files first
        boolean employeeLoaded = loadCSV(EMPLOYEE_FILE, employees);
        boolean attendanceLoaded = loadCSV(ATTENDANCE_FILE, attendance);

        if (!employeeLoaded || !attendanceLoaded) {
            return;
        }

        // ---------------- LOGIN ----------------
        System.out.print("Enter username: ");
        String username = cleanField(sc.nextLine());

        System.out.print("Enter password: ");
        String password = cleanField(sc.nextLine());

        if (!password.equals("12345") ||
                (!username.equals("employee") && !username.equals("payroll_staff"))) {
            System.out.println("Incorrect username and/or password.");
            return;
        }

        // ---------------- EMPLOYEE MENU ----------------
        if (username.equals("employee")) {
            System.out.println("1. Enter your employee number");
            System.out.println("2. Exit the program");
            System.out.print("Choose an option: ");

            int choice = safeParseInt(sc.nextLine());

            if (choice == 1) {
                System.out.print("Enter your employee number: ");
                String empNum = cleanField(sc.nextLine());

                String[] emp = findEmployee(employees, empNum);

                if (emp == null) {
                    System.out.println("Employee number does not exist.");
                    return;
                }

                System.out.println("Employee Number: " + cleanField(emp[0]));
                System.out.println("Employee Name: " + cleanField(emp[2]) + " " + cleanField(emp[1]));
                System.out.println("Birthday: " + cleanField(emp[3]));
            }

            return;
        }

        // ---------------- PAYROLL STAFF MENU ----------------
        if (username.equals("payroll_staff")) {
            System.out.println("1. Process Payroll");
            System.out.println("2. Exit the program");
            System.out.print("Choose an option: ");

            int choice = safeParseInt(sc.nextLine());

            if (choice != 1) {
                return;
            }

            // ------------- PROCESS PAYROLL SUBMENU -------------
            System.out.println("1. One employee");
            System.out.println("2. All employees");
            System.out.println("3. Exit the program");
            System.out.print("Choose an option: ");

            int subChoice = safeParseInt(sc.nextLine());

            if (subChoice == 1) {
                System.out.print("Enter employee number: ");
                String empNum = cleanField(sc.nextLine());

                String[] emp = findEmployee(employees, empNum);

                if (emp == null) {
                    System.out.println("Employee number does not exist.");
                    return;
                }

                displayPayroll(emp, attendance);
                return;
            }

            if (subChoice == 2) {
                for (int i = 1; i < employees.size(); i++) {
                    String[] emp = employees.get(i);
                    if (emp.length > 0) {
                        displayPayroll(emp, attendance);
                    }
                }
                return;
            }

            return;
        }
    }
}
