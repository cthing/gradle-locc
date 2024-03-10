#include "app.h"
#include <cassert>

int main() {
    project3::Greeter greeter;
    assert(greeter.greeting().compare("Hello, World!") == 0);
    return 0;
}
