public class Record {
    private String name;
    private String gender;
    private String age;

    public Record(String name, String gender, String age) {
        this.name = name;
        this.gender = gender;
        this.age = age;
    }

    @Override
    public String toString() {
        return "Record{name='" + name + "', gender='" + gender + "', age='" + age + "'}";
    }
    
    // Getters and Setters (if needed)
}
