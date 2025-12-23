package com.example.elderlycare;

public class MedicineModel {

    private String id;
    private String medicineName;
    private String dosage;
    private String timings;    // e.g. "1-0-1" or "09:00"
    private String duration;   // e.g. "5 days"

    private long startDate;      // epoch millis
    private boolean takenToday;  // legacy flag (kept for backwards compat)
    private int totalDays;       // parsed from duration
    private int daysCompleted;   // how many days fully completed
    // NEW fields for dose-level tracking:
    private boolean doseMorningRequired;
    private boolean doseAfternoonRequired;
    private boolean doseNightRequired;

    private boolean takenMorning;   // today's state for morning dose
    private boolean takenAfternoon;
    private boolean takenNight;

    private String lastCompletedDate; // "dd-MM-yyyy" - date when daysCompleted was last incremented

    public MedicineModel() {
        // required for Firebase
    }

    public MedicineModel(String name, String dosage, String timings, String duration) {
        this.medicineName = name;
        this.dosage = dosage;
        this.timings = timings;
        this.duration = duration;

        try {
            this.totalDays = Integer.parseInt(duration.replaceAll("\\D+", ""));
        } catch (Exception e) {
            this.totalDays = 1;
        }

        this.startDate = System.currentTimeMillis();
        this.takenToday = false;
        this.daysCompleted = 0;

        // parse pattern by default
        parseTimingsToDoses(timings);

        this.takenMorning = false;
        this.takenAfternoon = false;
        this.takenNight = false;
        this.lastCompletedDate = "";
    }

    // ---- parsing helper ----
    public void parseTimingsToDoses(String timings) {
        doseMorningRequired = false;
        doseAfternoonRequired = false;
        doseNightRequired = false;

        if (timings == null) return;

        timings = timings.trim();

        if (timings.contains("-")) {
            String[] p = timings.split("-");
            if (p.length >= 3) {
                doseMorningRequired = "1".equals(p[0]);
                doseAfternoonRequired = "1".equals(p[1]);
                doseNightRequired = "1".equals(p[2]);
            }
        } else {
            // single time -> try to map to morning/afternoon/night by hour
            try {
                String hhmm = timings;
                String[] sp = hhmm.split(":");
                int h = Integer.parseInt(sp[0]);
                if (h >= 5 && h < 12) doseMorningRequired = true;
                else if (h >= 12 && h < 17) doseAfternoonRequired = true;
                else doseNightRequired = true;
            } catch (Exception ignored) {
                // fallback: morning
                doseMorningRequired = true;
            }
        }
    }

    // ---------- GETTERS ----------
    public String getId() { return id; }
    public String getMedicineName() { return medicineName; }
    public String getDosage() { return dosage; }
    public String getTimings() { return timings; }
    public String getDuration() { return duration; }
    public int getTotalDays() { return totalDays; }
    public int getDaysCompleted() { return daysCompleted; }
    public long getStartDate() { return startDate; }
    public boolean isTakenToday() { return takenToday; }

    public boolean isDoseMorningRequired() { return doseMorningRequired; }
    public boolean isDoseAfternoonRequired() { return doseAfternoonRequired; }
    public boolean isDoseNightRequired() { return doseNightRequired; }

    public boolean isTakenMorning() { return takenMorning; }
    public boolean isTakenAfternoon() { return takenAfternoon; }
    public boolean isTakenNight() { return takenNight; }

    public String getLastCompletedDate() { return lastCompletedDate; }

    // ---------- SETTERS ----------
    public void setId(String id) { this.id = id; }
    public void setStartDate(long startDate) { this.startDate = startDate; }
    public void setTakenToday(boolean takenToday) { this.takenToday = takenToday; }
    public void setTotalDays(int totalDays) { this.totalDays = totalDays; }
    public void setDaysCompleted(int daysCompleted) { this.daysCompleted = daysCompleted; }

    public void setDoseMorningRequired(boolean v) { this.doseMorningRequired = v; }
    public void setDoseAfternoonRequired(boolean v) { this.doseAfternoonRequired = v; }
    public void setDoseNightRequired(boolean v) { this.doseNightRequired = v; }

    public void setTakenMorning(boolean v) { this.takenMorning = v; }
    public void setTakenAfternoon(boolean v) { this.takenAfternoon = v; }
    public void setTakenNight(boolean v) { this.takenNight = v; }

    public void setLastCompletedDate(String date) { this.lastCompletedDate = date; }
}
