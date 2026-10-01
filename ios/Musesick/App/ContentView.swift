import SwiftUI

public struct ContentView: View {
    @StateObject private var viewModel = MusicViewModel()

    public init() {}

    public var body: some View {
        ZStack {
            // Keep YouTube WebKit player engine mounted, opaque, and active behind the opaque UI
            YouTubePlayerBackgroundView()
                .frame(width: 120, height: 120)
                .opacity(1.0)
                .allowsHitTesting(false)

            HomeView(viewModel: viewModel)
        }
        .preferredColorScheme(.dark)
        .onOpenURL { url in
            NSLog("[ContentView] Received incoming URL: %@", url.absoluteString)
            viewModel.handleIncomingURL(url)
        }
    }
}
