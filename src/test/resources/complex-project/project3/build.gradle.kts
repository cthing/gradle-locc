plugins {
    // Apply the cpp-application plugin to add support for building C++ executables
    `cpp-application`

    // Apply the cpp-unit-test plugin to add support for building and running C++ test executables
    `cpp-unit-test`
}

// Set the target operating system and architecture for this application
application {
    targetMachines.add(machines.linux.x86_64)
}
