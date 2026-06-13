import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static File findExecutable(String command) {
        String path = System.getenv("PATH");

        if (path == null) {
            return null;
        }

        String[] pathDirs = path.split(File.pathSeparator);

        for (String dir : pathDirs) {
            File file = new File(dir, command);

            if (file.exists() && file.canExecute()) {
                return file;
            }
        }

        return null;
    }

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        String currentDirectory = System.getProperty("user.dir");

        while (true) {
            System.out.print("$ ");

            String command = sc.nextLine();

            if (command.equals("exit")) {
                break;
            }

            else if (command.startsWith("echo ")) {
                System.out.println(command.substring(5));
            }

            else if (command.startsWith("type ")) {
                String input = command.substring(5);

                if (input.equals("exit")
                        || input.equals("echo")
                        || input.equals("type")
                        || input.equals("pwd")
                        || input.equals("cd")) {

                    System.out.println(input + " is a shell builtin");
                } else {
                    File executable = findExecutable(input);

                    if (executable != null) {
                        System.out.println(input + " is " + executable.getAbsolutePath());
                    } else {
                        System.out.println(input + ": not found");
                    }
                }
            }

            else if (command.equals("pwd")) {
                System.out.println(currentDirectory);
            }

            else if (command.startsWith("cd ")) {
                String target = command.substring(3);

                File dir = new File(currentDirectory, target);;

                if (dir.exists() && dir.isDirectory()) {
                    currentDirectory = dir.getAbsolutePath();
                } else {
                    System.out.println("cd: " + target + ": No such file or directory");
                }
            }

            else {
                String[] parts = command.split(" ");
                File executable = findExecutable(parts[0]);

                if (executable == null) {
                    System.out.println(parts[0] + ": command not found");
                } else {
                    List<String> cmd = new ArrayList<>();
                    cmd.add(parts[0]);

                    for (int i = 1; i < parts.length; i++) {
                        cmd.add(parts[i]);
                    }

                    Process process = new ProcessBuilder(cmd)
                            .directory(new File(currentDirectory))
                            .inheritIO()
                            .start();
                    process.waitFor();
                }
            }
        }

        sc.close();
    }
}