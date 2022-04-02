public class JavaBuilderUser {
    private String name;
    private int age;
    private String address;
    private String gender;
    private boolean isMarried;
    private String education;
    private String nationality;
    private String belief;
    private String phone;

    JavaBuilderUser(String name, int age, String address, String gender, boolean isMarried, String education, String nationality, String belief, String phone) {
        this.name = name;
        this.age = age;
        this.address = address;
        this.gender = gender;
        this.isMarried = isMarried;
        this.education = education;
        this.nationality = nationality;
        this.belief = belief;
        this.phone = phone;

    }

    public static final class Builder{
        private String name;
        private int age;
        private String address;

        public Builder() {

        }

        public Builder buildName(String name){
            this.name = name;
            return this;
        }

        public Builder buildAddress(String address){
            this.address = address;
            return this;
        }

        public Builder buildAge(int age){
            this.age = age;
            return this;
        }

        public JavaBuilderUser build(){
            return new JavaBuilderUser(name, age, address, "man", false, "本科", "CN", "康米","123");
        }
    }
}
