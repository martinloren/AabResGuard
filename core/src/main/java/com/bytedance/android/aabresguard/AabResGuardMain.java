package com.bytedance.android.aabresguard;


import com.android.tools.build.bundletool.flags.FlagParser;
import com.android.tools.build.bundletool.flags.ParsedFlags;
import com.bytedance.android.aabresguard.commands.CommandHelp;
import com.bytedance.android.aabresguard.commands.DuplicatedResourcesMergerCommand;
import com.bytedance.android.aabresguard.commands.FileFilterCommand;
import com.bytedance.android.aabresguard.commands.ObfuscateBundleCommand;
import com.bytedance.android.aabresguard.commands.StringFilterCommand;
import com.bytedance.android.aabresguard.model.version.AabResGuardVersion;
import com.google.common.collect.ImmutableList;

import java.util.Optional;

/**
 * Main entry point of the AabResGuard.
 * <p>
 * Created by YangJing on 2019/10/09 .
 * Email: yangjing.yeoh@bytedance.com
 */
public class AabResGuardMain {
    private static final String HELP_CMD = "help";

    public static void main(String[] args) {
        System.exit(execute(args));
    }

    /**
     * Parses the flags and routes to the appropriate commands handler.
     * Returns the exit code instead of calling System.exit so it can be tested safely.
     */
    static int execute(String[] args) {
        final ParsedFlags flags;
        try {
            flags = new FlagParser().parse(args);
        } catch (FlagParser.FlagParseException e) {
            System.err.println("Error while parsing the flags: " + e.getMessage());
            return 1;
        }
        Optional<String> command = flags.getMainCommand();
        if (!command.isPresent()) {
            System.err.println("Error: You have to specify a commands.");
            help();
            return 1;
        }
        try {
            switch (command.get()) {
                case ObfuscateBundleCommand.COMMAND_NAME:
                    ObfuscateBundleCommand.fromFlags(flags).execute();
                    break;
                case DuplicatedResourcesMergerCommand.COMMAND_NAME:
                    DuplicatedResourcesMergerCommand.fromFlags(flags).execute();
                    break;
                case FileFilterCommand.COMMAND_NAME:
                    FileFilterCommand.fromFlags(flags).execute();
                    break;
                case StringFilterCommand.COMMAND_NAME:
                    StringFilterCommand.fromFlags(flags).execute();
                    break;
                case HELP_CMD:
                    if (flags.getSubCommand().isPresent()) {
                        return help(flags.getSubCommand().get());
                    } else {
                        help();
                    }
                    break;
                default:
                    System.err.printf("Error: Unrecognized command '%s'.%n%n%n", command.get());
                    help();
                    return 1;
            }
        } catch (Exception e) {
            System.err.println(
                    "[BT:" + AabResGuardVersion.getCurrentVersion() + "] Error: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
        return 0;
    }

    /**
     * Displays a general help.
     */
    public static void help() {
        ImmutableList<CommandHelp> commandHelps =
                ImmutableList.of(
                        ObfuscateBundleCommand.help(),
                        DuplicatedResourcesMergerCommand.help(),
                        FileFilterCommand.help(),
                        StringFilterCommand.help()
                );
        System.out.println("Synopsis: aabResGuard <command> ...");
        System.out.println();
        System.out.println("Use 'aabResGuard help <command>' to learn more about the given command.");
        System.out.println();
        commandHelps.forEach(commandHelp -> commandHelp.printSummary(System.out));
    }

    /**
     * Displays help about a given commands.
     */
    public static int help(String commandName) {
        CommandHelp commandHelp;
        switch (commandName) {
            case ObfuscateBundleCommand.COMMAND_NAME:
                commandHelp = ObfuscateBundleCommand.help();
                break;
            case DuplicatedResourcesMergerCommand.COMMAND_NAME:
                commandHelp = DuplicatedResourcesMergerCommand.help();
                break;
            case FileFilterCommand.COMMAND_NAME:
                commandHelp = FileFilterCommand.help();
                break;
                case StringFilterCommand.COMMAND_NAME:
                commandHelp = StringFilterCommand.help();
                break;
            default:
                System.err.printf("Error: Unrecognized command '%s'.%n%n%n", commandName);
                help();
                return 1;
        }
        commandHelp.printDetails(System.out);
        return 0;
    }
}
