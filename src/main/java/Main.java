import java.io.File;
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
             else if (command.startsWith("type ")) {
                String input = command.substring(5);

                if (input.equals("exit") || input.equals("echo") || input.equals("type")) {
                    System.out.println(input + " is a shell builtin");
                } 
                else {
                    String[] pathDirs = System.getenv("PATH").split(File.pathSeparator);
                    boolean found = false;

                    for (String dir : pathDirs) {
                        File file = new File(dir, input);

                        if (file.exists() && file.canExecute()) {
                        System.out.println(input + " is " + file.getAbsolutePath());
                        found = true;
                        break;
                    }
                }

                    if (!found) {
                    System.out.println(input + ": not found");
                    }
                }
            }

            else {
                System.out.println(command + ": command not found");
            }
        }

                     
    }    
        
}
