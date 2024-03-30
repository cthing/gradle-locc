plugins {
    // Apply the swift-library plugin to add support for building Swift libraries
    `swift-library`

    // Apply the xctest plugin to add support for building and running Swift test executables (Linux) or bundles (macOS)
    xctest
}

library {
    // Set the target operating system and architecture for this library
    targetMachines.add(machines.linux.x86_64)
}
