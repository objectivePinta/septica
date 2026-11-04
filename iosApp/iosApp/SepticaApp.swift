import SwiftUI

@main
struct SepticaApp: App {
    @AppStorage("ro.septica.app.theme") private var selectedThemeId = "classic_romanian"

    var body: some Scene {
        WindowGroup {
            ContentView()
                .preferredColorScheme(selectedThemeId == "minimal" ? .light : .dark)
        }
    }
}
