import SwiftUI

public struct HomeView: View {
    @ObservedObject var viewModel: MusicViewModel
    @State private var selectedTab = 0

    @MainActor
    public init(viewModel: MusicViewModel) {
        self.viewModel = viewModel
    }

    public var body: some View {
        ZStack(alignment: .bottom) {
            // Tab View
            TabView(selection: $selectedTab) {
                StreamView(viewModel: viewModel)
                    .tabItem {
                        Label("Stream", systemImage: "sparkles")
                    }
                    .tag(0)

                LocalTracksView(viewModel: viewModel)
                    .tabItem {
                        Label("Local", systemImage: "music.note.house")
                    }
                    .tag(1)

                PlaylistsView(viewModel: viewModel)
                    .tabItem {
                        Label("Playlists", systemImage: "music.note.list")
                    }
                    .tag(2)

                SettingsView(viewModel: viewModel)
                    .tabItem {
                        Label("Settings", systemImage: "gearshape")
                    }
                    .tag(3)
            }
            .accentColor(.white)

            // Floating MiniPlayerBar
            if viewModel.playerManager.state.currentTrack != nil {
                MiniPlayerBar(playerManager: viewModel.playerManager) {
                    viewModel.showFullPlayer = true
                }
                .padding(.bottom, 54)
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .fullScreenCover(isPresented: $viewModel.showFullPlayer) {
            FullPlayerView(playerManager: viewModel.playerManager)
        }
        .sheet(item: $viewModel.selectedArtist) { artist in
            NavigationStack {
                ArtistDetailView(artist: artist, viewModel: viewModel)
            }
        }
        .sheet(item: $viewModel.selectedAlbum) { album in
            NavigationStack {
                AlbumDetailView(album: album, viewModel: viewModel)
            }
        }
    }
}
