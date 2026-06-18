import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.util.Iterator;

public class Main {
    static class Job {
        int jobId;
        Process process;
        String command;
        boolean showDone = false;

        Job(int jobId,
                Process process,
                String command) {
            this.jobId = jobId;
            this.process = process;
            this.command = command;
        }
    }

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
        boolean escaped = false;

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);

            // Backslash escaping outside quotes
            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }

            if (!inSingleQuote && !inDoubleQuote && c == '\\') {
                escaped = true;
                continue;
            }

            // Backslashes inside double quotes
            if (inDoubleQuote && c == '\\') {

                if (i + 1 < command.length()) {
                    char next = command.charAt(i + 1);

                    if (next == '"' || next == '\\') {
                        current.append(next);
                        i++; // skip next character
                        continue;
                    }
                }

                // keep the backslash literally
                current.append('\\');
                continue;
            }

            // Single quote handling
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }

            // Double quote handling
            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }

            // Token separator
            if (c == ' ' && !inSingleQuote && !inDoubleQuote) {

                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }

                continue;
            }

            current.append(c);
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        return tokens;
    }

    private static void reapJobs(List<Job> jobs) {

        Job lastJob = jobs.isEmpty() ? null : jobs.get(jobs.size() - 1);
        Job secondLastJob = jobs.size() >= 2
                ? jobs.get(jobs.size() - 2)
                : null;

        Iterator<Job> iterator = jobs.iterator();

        while (iterator.hasNext()) {

            Job job = iterator.next();

            if (!job.process.isAlive()) {

                String marker = "";

                if (job == lastJob) {
                    marker = "+";
                } else if (job == secondLastJob) {
                    marker = "-";
                }

                System.out.printf(
                        "[%d]%s  %-24s %s%n",
                        job.jobId,
                        marker,
                        "Done",
                        job.command.replace(" &", ""));

                iterator.remove();
            }
        }
    }

    private static int getNextJobId(List<Job> jobs) {

        int id = 1;

        while (true) {

            boolean used = false;

            for (Job job : jobs) {
                if (job.jobId == id) {
                    used = true;
                    break;
                }
            }

            if (!used) {
                return id;
            }

            id++;
        }
    }

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        String currentDirectory = System.getProperty("user.dir");

        List<Job> jobs = new ArrayList<>();

        while (true) {
            reapJobs(jobs);
            System.out.print("$ ");

            String command = sc.nextLine();
            List<String> parts = parseCommand(command);

            String outputFile = null;
            String errorFile = null;

            boolean appendOutput = false;
            boolean appendError = false;

            boolean background = false;

            for (int i = 0; i < parts.size(); i++) {

                String token = parts.get(i);

                if (token.equals(">") || token.equals("1>")) {
                    outputFile = parts.get(i + 1);
                    appendOutput = false;

                    parts = new ArrayList<>(parts.subList(0, i));
                    break;
                }

                else if (token.equals(">>") || token.equals("1>>")) {
                    outputFile = parts.get(i + 1);
                    appendOutput = true;

                    parts = new ArrayList<>(parts.subList(0, i));
                    break;
                }

                else if (token.equals("2>")) {
                    errorFile = parts.get(i + 1);
                    appendError = false;

                    parts = new ArrayList<>(parts.subList(0, i));
                    break;
                }

                else if (token.equals("2>>")) {
                    errorFile = parts.get(i + 1);
                    appendError = true;

                    parts = new ArrayList<>(parts.subList(0, i));
                    break;
                }

                if (!parts.isEmpty() && parts.get(parts.size() - 1).equals("&")) {
                    background = true;
                    parts.remove(parts.size() - 1);
                }
            }

            if (command.contains("|")) {

                String[] pipeParts = command.split("\\|", 2);

                List<String> left = parseCommand(pipeParts[0].trim());

                List<String> right = parseCommand(pipeParts[1].trim());

                Process p1 = new ProcessBuilder(left)
                        .directory(new File(currentDirectory))
                        .start();

                Process p2 = new ProcessBuilder(right)
                        .directory(new File(currentDirectory))
                        .start();

                Thread pipeThread = new Thread(() -> {
                    try {

                        p1.getInputStream()
                                .transferTo(
                                        p2.getOutputStream());

                        p2.getOutputStream().close();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                pipeThread.start();

                p2.getInputStream()
                        .transferTo(System.out);

                p1.waitFor();
                p2.waitFor();
                pipeThread.join();

                continue;
            }

            else if (command.equals("exit")) {
                break;
            }

            else if (!parts.isEmpty() && parts.get(0).equals("echo")) {

                String result = "";

                if (parts.size() > 1) {
                    result = String.join(" ", parts.subList(1, parts.size()));
                }

                if (outputFile != null) {

                    FileWriter fw = new FileWriter(outputFile, appendOutput);

                    PrintWriter writer = new PrintWriter(fw);

                    writer.println(result);
                    writer.close();

                } else {

                    System.out.println(result);
                }

                if (errorFile != null) {
                    new PrintWriter(errorFile).close();
                }
            }

            else if (command.startsWith("type ")) {
                String input = command.substring(5);

                if (input.equals("exit")
                        || input.equals("echo")
                        || input.equals("type")
                        || input.equals("pwd")
                        || input.equals("cd")
                        || input.equals("jobs")) {

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

            else if (command.equals("jobs")) {

                Job lastJob = jobs.size() >= 1
                        ? jobs.get(jobs.size() - 1)
                        : null;

                Job secondLastJob = jobs.size() >= 2
                        ? jobs.get(jobs.size() - 2)
                        : null;

                List<Job> toRemove = new ArrayList<>();

                for (Job job : jobs) {

                    String marker = "";

                    if (job == lastJob) {
                        marker = "+";
                    } else if (job == secondLastJob) {
                        marker = "-";
                    }

                    if (job.process.isAlive()) {

                        System.out.printf(
                                "[%d]%s  %-24s %s%n",
                                job.jobId,
                                marker,
                                "Running",
                                job.command);

                    } else {

                        System.out.printf(
                                "[%d]%s  %-24s %s%n",
                                job.jobId,
                                marker,
                                "Done",
                                job.command.replace(" &", ""));

                        toRemove.add(job);
                    }
                }

                jobs.removeAll(toRemove);
            } else {

                File executable = findExecutable(parts.get(0));

                if (executable == null) {
                    System.out.println(parts.get(0) + ": command not found");
                } else {
                    List<String> cmd = new ArrayList<>();
                    cmd.addAll(parts);

                    ProcessBuilder pb = new ProcessBuilder(cmd);
                    pb.directory(new File(currentDirectory));

                    if (outputFile == null) {
                        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                    } else {
                        if (appendOutput) {
                            pb.redirectOutput(
                                    ProcessBuilder.Redirect.appendTo(
                                            new File(outputFile)));
                        } else {
                            pb.redirectOutput(
                                    new File(outputFile));
                        }
                    }

                    if (errorFile == null) {
                        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                    } else {
                        if (appendError) {
                            pb.redirectError(
                                    ProcessBuilder.Redirect.appendTo(
                                            new File(errorFile)));
                        } else {
                            pb.redirectError(
                                    new File(errorFile));
                        }
                    }

                    Process process = pb.start();
                    int jobId = getNextJobId(jobs);

                    if (background) {

                        jobs.add(new Job(jobId, process, command));

                        System.out.println(
                                "[" + jobId + "] " +
                                        process.pid());

                        jobId++;

                    } else {

                        process.waitFor();

                    }
                }
            }
        }

        sc.close();
    }
}