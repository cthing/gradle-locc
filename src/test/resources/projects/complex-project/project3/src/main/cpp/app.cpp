#include <iostream>
#include <stdlib.h>
#include "app.h"

std::string project3::Greeter::greeting() {
    return std::string("Hello, World!");
}

int main () {
    project3::Greeter greeter;
    std::cout << greeter.greeting() << std::endl;
    return 0;
}
