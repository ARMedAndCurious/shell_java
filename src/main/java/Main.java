import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        // TODO: Uncomment the code below to pass the first stage
        boolean flag = true;
        
        while(flag){
            System.out.print("$ ");
            
            Scanner sc = new Scanner(System.in);
            String command = sc.nextLine();
    
            if(command=="exit") {
                flag=false;
                break;
            }
            System.out.println(command+": command not found");

        }
    }
}
