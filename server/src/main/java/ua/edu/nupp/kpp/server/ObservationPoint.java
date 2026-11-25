package ua.edu.nupp.kpp.server;

public class ObservationPoint {

    private long id;
    private String code;
    private String name;
    private String location;
    private String waterUse;
    private int sensitivityScore;

    public ObservationPoint() {
    }

    public ObservationPoint(long id, String code, String name, String location, String waterUse, int sensitivityScore) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.location = location;
        this.waterUse = waterUse;
        this.sensitivityScore = sensitivityScore;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getWaterUse() {
        return waterUse;
    }

    public void setWaterUse(String waterUse) {
        this.waterUse = waterUse;
    }

    public int getSensitivityScore() {
        return sensitivityScore;
    }

    public void setSensitivityScore(int sensitivityScore) {
        this.sensitivityScore = sensitivityScore;
    }
}
