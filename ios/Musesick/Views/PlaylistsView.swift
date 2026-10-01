import SwiftUI

public struct PlaylistsView: View {
    @ObservedObject var viewModel: MusicViewModel
    @State private var showingCreateAlert = false
    @State private var newPlaylistTitle = ""

    public init(viewModel: MusicViewModel) {
        self.viewModel = viewModel
    }

    public var body: some View {
        NavigationStack {
            ZStack {
                Color.black.ignoresSafeArea()

                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        HStack {
                            Text("YOUR PLAYLISTS")
                                .font(.system(size: 13, weight: .bold))
                                .tracking(1.2)
                                .foregroundColor(.white.opacity(0.7))

                            Spacer()

                            Button(action: { showingCreateAlert = true }) {
                                HStack(spacing: 4) {
                                    Image(systemName: "plus")
                                    Text("New Playlist")
                                }
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(.white)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(Color.white.opacity(0.18))
                                .clipShape(Capsule())
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.top, 8)

                        ForEach(viewModel.userPlaylists) { playlist in
                            HStack(spacing: 14) {
                                RoundedRectangle(cornerRadius: 10)
                                    .fill(Color(white: 0.18))
                                    .frame(width: 56, height: 56)
                                    .overlay(
                                        Image(systemName: "music.note.list")
                                            .font(.system(size: 20))
                                            .foregroundColor(.white.opacity(0.6))
                                    )

                                VStack(alignment: .leading, spacing: 3) {
                                    Text(playlist.title)
                                        .font(.system(size: 16, weight: .bold))
                                        .foregroundColor(.white)

                                    Text("\(playlist.tracks.count) tracks")
                                        .font(.system(size: 13))
                                        .foregroundColor(.white.opacity(0.6))
                                }

                                Spacer()

                                Image(systemName: "chevron.right")
                                    .font(.system(size: 14))
                                    .foregroundColor(.white.opacity(0.4))
                            }
                            .padding(.horizontal, 16)
                            .padding(.vertical, 6)
                        }

                        Spacer().frame(height: 80)
                    }
                }
            }
            .navigationTitle("Playlists")
            .navigationBarTitleDisplayMode(.large)
            .toolbarBackground(Color.black, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .alert("Create Playlist", isPresented: $showingCreateAlert) {
                TextField("Playlist Name", text: $newPlaylistTitle)
                Button("Create") {
                    let title = newPlaylistTitle.trimmingCharacters(in: .whitespaces)
                    if !title.isEmpty {
                        let newP = Playlist(id: UUID().uuidString, title: title, trackCount: 0, tracks: [], isUserCreated: true)
                        viewModel.userPlaylists.append(newP)
                        newPlaylistTitle = ""
                    }
                }
                Button("Cancel", role: .cancel) {
                    newPlaylistTitle = ""
                }
            }
        }
    }
}
