////package com.sandhyyasofttech.attendsmart.Models;
////
////public class EmployeePunchModel {
////
////    public String mobile;
////    public String checkInTime, checkOutTime;
////    public String checkInAddress, checkOutAddress;
////    public String checkInPhoto, checkOutPhoto;
////    public Double checkInLat, checkInLng, checkOutLat, checkOutLng;
////
////    public EmployeePunchModel() {}
////}
//
//
//
//package com.sandhyyasofttech.attendsmart.Models;
//
//public class EmployeePunchModel {
//
//    public String mobile;
//    public String employeeName;
//    public String checkInTime, checkOutTime;
//    public String checkInAddress, checkOutAddress;
//    public String checkInPhoto, checkOutPhoto;
//    public Double checkInLat, checkInLng, checkOutLat, checkOutLng;
//
//    // Late marking
//    public boolean isLate;
//    public String workingHours;
//
//    public EmployeePunchModel() {}
//
//    /**
//     * Calculate if employee is late
//     * @param officeStartTime in format "HH:mm" (e.g., "09:30")
//     * @return true if late, false otherwise
//     */
//    public boolean calculateLateStatus(String officeStartTime) {
//        if (checkInTime == null || checkInTime.isEmpty()) {
//            return false;
//        }
//
//        try {
//            // Parse check-in time
//            String[] checkInParts = checkInTime.split(":");
//            int checkInHour = Integer.parseInt(checkInParts[0]);
//            int checkInMinute = Integer.parseInt(checkInParts[1].substring(0, 2));
//
//            // Handle AM/PM
//            if (checkInTime.toUpperCase().contains("PM") && checkInHour != 12) {
//                checkInHour += 12;
//            } else if (checkInTime.toUpperCase().contains("AM") && checkInHour == 12) {
//                checkInHour = 0;
//            }
//
//            // Parse office start time
//            String[] officeParts = officeStartTime.split(":");
//            int officeHour = Integer.parseInt(officeParts[0]);
//            int officeMinute = Integer.parseInt(officeParts[1]);
//
//            // Convert to minutes for comparison
//            int checkInMinutes = checkInHour * 60 + checkInMinute;
//            int officeMinutes = officeHour * 60 + officeMinute;
//
//            isLate = checkInMinutes > officeMinutes;
//            return isLate;
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    /**
//     * Calculate working hours
//     * @return formatted string like "9h 30m"
//     */
//    public String calculateWorkingHours() {
//        if (checkInTime == null || checkOutTime == null ||
//                checkInTime.isEmpty() || checkOutTime.isEmpty()) {
//            return null;
//        }
//
//        try {
//            // Parse check-in time
//            String[] checkInParts = checkInTime.split(":");
//            int checkInHour = Integer.parseInt(checkInParts[0]);
//            int checkInMinute = Integer.parseInt(checkInParts[1].substring(0, 2));
//
//            if (checkInTime.toUpperCase().contains("PM") && checkInHour != 12) {
//                checkInHour += 12;
//            } else if (checkInTime.toUpperCase().contains("AM") && checkInHour == 12) {
//                checkInHour = 0;
//            }
//
//            // Parse check-out time
//            String[] checkOutParts = checkOutTime.split(":");
//            int checkOutHour = Integer.parseInt(checkOutParts[0]);
//            int checkOutMinute = Integer.parseInt(checkOutParts[1].substring(0, 2));
//
//            if (checkOutTime.toUpperCase().contains("PM") && checkOutHour != 12) {
//                checkOutHour += 12;
//            } else if (checkOutTime.toUpperCase().contains("AM") && checkOutHour == 12) {
//                checkOutHour = 0;
//            }
//
//            // Calculate difference
//            int totalMinutes = (checkOutHour * 60 + checkOutMinute) -
//                    (checkInHour * 60 + checkInMinute);
//
//            if (totalMinutes < 0) {
//                totalMinutes += 24 * 60; // Handle overnight shifts
//            }
//
//            int hours = totalMinutes / 60;
//            int minutes = totalMinutes % 60;
//
//            workingHours = hours + "h " + minutes + "m";
//            return workingHours;
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//}


package com.sandhyyasofttech.attendsmart.Models;

public class EmployeePunchModel {

    public String mobile;
    public String employeeName;

    // Check-in data
    public String checkInTime;
    public String checkInAddress;
    public String checkInPhoto;
    public Double checkInLat;
    public Double checkInLng;
    public Boolean checkInGPS;

    // Check-out data
    public String checkOutTime;
    public String checkOutAddress;
    public String checkOutPhoto;
    public Double checkOutLat;
    public Double checkOutLng;
    public Boolean checkOutGPS;

    // Status from Firebase
    public String status; // "Late", "On Time", etc.

    // Calculated fields
    public boolean isLate;
    public String workingHours;
    public String lateStatus;

    public EmployeePunchModel() {}

    /**
     * Determine if employee is late based on status from Firebase
     */
    public void updateLateStatus() {
        if (status != null) {
            isLate = status.equalsIgnoreCase("Late");
        } else {
            isLate = false;
        }
    }

    /**
     * Calculate if employee is late (fallback if status not in Firebase)
     * @param officeStartTime in format "HH:mm" (e.g., "09:30")
     * @return true if late, false otherwise
     */
    public boolean calculateLateStatus(String officeStartTime) {
        // First check if status is already set in Firebase
        if (status != null && !status.isEmpty()) {
            isLate = status.equalsIgnoreCase("Late");
            return isLate;
        }

        // If no status in Firebase, calculate manually
        if (checkInTime == null || checkInTime.isEmpty()) {
            return false;
        }

        try {
            // Parse check-in time
            String timeOnly = checkInTime.trim();
            String[] parts = timeOnly.split(":");

            int checkInHour = Integer.parseInt(parts[0]);
            int checkInMinute = Integer.parseInt(parts[1].split(" ")[0]);

            // Handle AM/PM
            String amPm = timeOnly.toUpperCase().contains("PM") ? "PM" : "AM";
            if (amPm.equals("PM") && checkInHour != 12) {
                checkInHour += 12;
            } else if (amPm.equals("AM") && checkInHour == 12) {
                checkInHour = 0;
            }

            // Parse office start time
            String[] officeParts = officeStartTime.split(":");
            int officeHour = Integer.parseInt(officeParts[0]);
            int officeMinute = Integer.parseInt(officeParts[1]);

            // Convert to minutes for comparison
            int checkInMinutes = checkInHour * 60 + checkInMinute;
            int officeMinutes = officeHour * 60 + officeMinute;

            isLate = checkInMinutes > officeMinutes;
            return isLate;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Calculate working hours between check-in and check-out
     * @return formatted string like "9h 30m"
     */
    public String calculateWorkingHours() {
        if (checkInTime == null || checkOutTime == null ||
                checkInTime.isEmpty() || checkOutTime.isEmpty()) {
            return null;
        }

        try {
            // Parse check-in time
            String checkInTimeClean = checkInTime.trim();
            String[] checkInParts = checkInTimeClean.split(":");
            int checkInHour = Integer.parseInt(checkInParts[0]);
            int checkInMinute = Integer.parseInt(checkInParts[1].split(" ")[0]);

            String checkInAmPm = checkInTimeClean.toUpperCase().contains("PM") ? "PM" : "AM";
            if (checkInAmPm.equals("PM") && checkInHour != 12) {
                checkInHour += 12;
            } else if (checkInAmPm.equals("AM") && checkInHour == 12) {
                checkInHour = 0;
            }

            // Parse check-out time
            String checkOutTimeClean = checkOutTime.trim();
            String[] checkOutParts = checkOutTimeClean.split(":");
            int checkOutHour = Integer.parseInt(checkOutParts[0]);
            int checkOutMinute = Integer.parseInt(checkOutParts[1].split(" ")[0]);

            String checkOutAmPm = checkOutTimeClean.toUpperCase().contains("PM") ? "PM" : "AM";
            if (checkOutAmPm.equals("PM") && checkOutHour != 12) {
                checkOutHour += 12;
            } else if (checkOutAmPm.equals("AM") && checkOutHour == 12) {
                checkOutHour = 0;
            }

            // Calculate difference
            int totalMinutes = (checkOutHour * 60 + checkOutMinute) -
                    (checkInHour * 60 + checkInMinute);

            if (totalMinutes < 0) {
                totalMinutes += 24 * 60; // Handle overnight shifts
            }

            int hours = totalMinutes / 60;
            int minutes = totalMinutes % 60;

            workingHours = hours + "h " + minutes + "m";
            return workingHours;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Check if employee has checked out
     */
    public boolean hasCheckedOut() {
        return checkOutTime != null && !checkOutTime.isEmpty();
    }

    /**
     * Check if employee has checked in
     */
    public boolean hasCheckedIn() {
        return checkInTime != null && !checkInTime.isEmpty();
    }
}