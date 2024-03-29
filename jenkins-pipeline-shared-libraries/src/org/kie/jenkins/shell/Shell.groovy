package org.kie.jenkins.shell

import org.kie.jenkins.shell.installation.Installation

/**
 * Shell interface defines the methods that a shell should implement
 * It allows to manage installations of binaries on the a system (local, remote or whatever could be useful).
 *
 * Use `install` method to add new binaries to local shell.
 *
 * All commands using local binaries should be done via the `execute*` methods.
 *
*/
interface Shell {

    void enableDebug()

    void execute(String command)
    void execute(String command, String directory)

    String executeWithOutput(String command)
    String executeWithOutput(String command, String directory)

    def executeWithStatus(String command)
    def executeWithStatus(String command, String directory)

    void addEnvironmentVariable(String key, String value)

    Map getEnvironmentVariables()

    void install(Installation installation)

}
