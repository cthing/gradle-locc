import XCTest
@testable import Lib

class HelloTests: XCTestCase {
    public static var allTests = [
        ("testGreeting", testGreeting),
    ]

    func testGreeting() {
        XCTAssertEqual("Hello, World!", Hello().greeting())
    }
}
