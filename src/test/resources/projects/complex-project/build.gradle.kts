plugins {
    id("org.cthing.locc")
}

tasks {
    countCodeLines {
        reports {
            xml.required = true
            html.required = true
            yaml.required = true
            json.required = true
            csv.required = true
            text.required = true
            console.required = true
        }
    }
}
