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

    private static List<String> parseCommand(String command) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (char c : command.toCharArray()) {

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (c == ' ' && !inSingleQuote && !inDoubleQuote) {

                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }

            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        return tokens;
    }

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        String currentDirectory = System.getProperty("user.dir");

        while (true) {
            System.out.print("$ ");

            String command = sc.nextLine();
            List<String> parts = parseCommand(command);

            if (command.equals("exit")) {
                break;
            }

            else if (!parts.isEmpty() && parts.get(0).equals("echo")) {

                if (parts.size() > 1) {
                    System.out.println(String.join(" ", parts.subList(1, parts.size())));
                } else {
                    System.out.println();
                }
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

                if (target.equals("~")) {
                    target = System.getenv("HOME");
                }

                File dir;

                if (target.startsWith("/")) {
                    dir = new File(target); // absolute path
                } else {
                    dir = new File(currentDirectory, target); // relative path
                }

                dir = dir.getCanonicalFile();

                if (dir.exists() && dir.isDirectory()) {
                    currentDirectory = dir.getAbsolutePath();
                } else {
                    System.out.println("cd: " + target + ": No such file or directory");
                }
            }

            else {

                File executable = findExecutable(parts.get(0));

                if (executable == null) {
                    System.out.println(parts.get(0) + ": command not found");
                } else {
                    List<String> cmd = new ArrayList<>();
                    cmd.addAll(parts);

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