import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        // TODO: Uncomment the code below to pass the first stage
        boolean flag = true;
        
        while(flag){
            System.out.print("$ ");
            
            Scanner sc = new Scanner(System.in);
            String command = sc.nextLine();
    
            if(command.equals("exit")) {
               
               break;
            }
            else if(command.startsWith("echo ")) {
                System.out.println(command.substring(5));
            }
             else if(command.startsWith("type ")) {
                String input =command.substring(5);
                if(input.equals("exit") || input.equals("echo") )
                     System.out.println(input+" is a shell builtin");
                else System.out.println(input+": not found");    
            }
            else System.out.println(command+": command not found");

        }
    }
}
