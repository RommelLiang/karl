package behavioral;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class JavaIterable {
    public static void main(String[] args) {
        UserList userList = new UserList(new ArrayList(Arrays.asList("K", "A", "R", "L")));
        while (userList.hasNext()) {
            String next = userList.next();
            System.out.println(next);
            if (next.equals("A")) {
                userList.remove();
            }
        }
        userList.println();
    }

    static class UserList implements Iterator<String>{

        private List<String> names;
        private Iterator<String>  mIterator;

        public UserList(List<String> names) {
            this.names = names;
            this.mIterator = names.iterator();
        }

        @Override
        public boolean hasNext() {
            return mIterator.hasNext();
        }

        @Override
        public String next() {
            return mIterator.next();
        }

        @Override
        public void remove() {
            mIterator.remove();
        }

        public void println(){
            for (String name : names) {
                System.out.print(name);
            }

        }
    }

}
