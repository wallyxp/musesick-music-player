import SwiftUI

public struct ContentView: View {
    public init() {}

    public var body: some View {
        ZStack {
            HomeView()

            // Keep YouTube WebKit player engine mounted and active in view hierarchy
            YouTubePlayerBackgroundView()
                .frame(width: 200, height: 200)
                .opacity(0.001)
                .allowsHitTesting(false)
        }
        .preferredColorScheme(.dark)
    }
}
