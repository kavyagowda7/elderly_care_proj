package com.example.elderlycare;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Prescription {
    public String id;
    public Object createdAt;
    public List<MedicineModel> medicines = new ArrayList<>();

    public static Prescription fromMap(Map<String,Object> data, String id) {
        Prescription p = new Prescription();
        p.id = id;
        p.createdAt = data.get("createdAt");
        Object medsObj = data.get("medicines");
        if (medsObj instanceof List) {
            List list = (List) medsObj;
            for (Object o : list) {
                if (o instanceof Map) {
                    Map m = (Map) o;
                    String name = m.get("name") == null ? "" : m.get("name").toString();
                    String dosage = m.get("dosage") == null ? "" : m.get("dosage").toString();
                    String timings = m.get("timings") == null ? "" : m.get("timings").toString();
                    String duration = m.get("duration") == null ? "" : m.get("duration").toString();
                    p.medicines.add(new MedicineModel(name, dosage, timings, duration));
                }
            }
        }
        return p;
    }
}
