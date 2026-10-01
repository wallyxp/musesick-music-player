import SwiftUI

public struct FavoriteArtistsSelectionView: View {
    @ObservedObject var viewModel: MusicViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var searchText = ""
    @State private var selectedArtistMap: [String: Artist] = [:]
    @State private var searchTask: Task<Void, Never>? = nil

    // Curated list of popular artists for fast onboarding
    private var popularArtists: [Artist] { Artist.popularArtists }

    private let columns = [
        GridItem(.flexible(), spacing: 14),
        GridItem(.flexible(), spacing: 14),
        GridItem(.flexible(), spacing: 14)
    ]

    public init(viewModel: MusicViewModel) {
        self.viewModel = viewModel
    }

    public var body: some View {
        NavigationStack {
            ZStack {
                Color.black.ignoresSafeArea()

                VStack(spacing: 0) {
                    // Header
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Pick Your Favourite Artists")
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(.white)

                        Text("Select artists you like to customize your Stream and recommendations.")
                            .font(.system(size: 13))
                            .foregroundColor(.white.opacity(0.65))
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 20)
                    .padding(.top, 14)
                    .padding(.bottom, 12)

                    // Search Field
                    HStack(spacing: 10) {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(.white.opacity(0.5))

                        TextField("Search any artist...", text: $searchText)
                            .foregroundColor(.white)
                            .autocorrectionDisabled()
                            .textInputAutocapitalization(.never)
                            .onChange(of: searchText) { newValue in
                                triggerSearch(query: newValue)
                            }

                        if !searchText.isEmpty {
                            Button(action: {
                                searchText = ""
                                viewModel.searchedArtists = []
                            }) {
                                Image(systemName: "xmark.circle.fill")
                                    .foregroundColor(.white.opacity(0.5))
                            }
                        }
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(Color(white: 0.14))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .padding(.horizontal, 20)
                    .padding(.bottom, 10)

                    // Currently selected count badge
                    if !selectedArtistMap.isEmpty {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(Array(selectedArtistMap.values), id: \.id) { artist in
                                    HStack(spacing: 6) {
                                        Text(artist.name)
                                            .font(.system(size: 12, weight: .medium))
                                            .foregroundColor(.white)

                                        Button(action: {
                                            selectedArtistMap.removeValue(forKey: artistKey(artist))
                                        }) {
                                            Image(systemName: "xmark")
                                                .font(.system(size: 10, weight: .bold))
                                                .foregroundColor(.white.opacity(0.7))
                                        }
                                    }
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 5)
                                    .background(Color.white.opacity(0.2))
                                    .clipShape(Capsule())
                                }
                            }
                            .padding(.horizontal, 20)
                        }
                        .padding(.bottom, 10)
                    }

                    // Artists Grid
                    ScrollView {
                        VStack(alignment: .leading, spacing: 14) {
                            if viewModel.isSearchingArtists {
                                HStack {
                                    Spacer()
                                    ProgressView()
                                        .tint(.white)
                                        .padding(.vertical, 30)
                                    Spacer()
                                }
                            } else if !searchText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                                if viewModel.searchedArtists.isEmpty {
                                    Text("No artists found for \"\(searchText)\"")
                                        .font(.system(size: 14))
                                        .foregroundColor(.white.opacity(0.5))
                                        .frame(maxWidth: .infinity, alignment: .center)
                                        .padding(.top, 40)
                                } else {
                                    Text("SEARCH RESULTS")
                                        .font(.system(size: 12, weight: .bold))
                                        .tracking(1.1)
                                        .foregroundColor(.white.opacity(0.6))
                                        .padding(.horizontal, 20)

                                    LazyVGrid(columns: columns, spacing: 18) {
                                        ForEach(viewModel.searchedArtists) { artist in
                                            artistCard(artist)
                                        }
                                    }
                                    .padding(.horizontal, 20)
                                }
                            } else {
                                Text("POPULAR ARTISTS")
                                    .font(.system(size: 12, weight: .bold))
                                    .tracking(1.1)
                                    .foregroundColor(.white.opacity(0.6))
                                    .padding(.horizontal, 20)

                                LazyVGrid(columns: columns, spacing: 18) {
                                    ForEach(displayedPopularArtists) { artist in
                                        artistCard(artist)
                                    }
                                }
                                .padding(.horizontal, 20)
                            }
                        }
                        .padding(.bottom, 90)
                    }

                    // Bottom Action Bar
                    VStack(spacing: 8) {
                        Button(action: saveAndDismiss) {
                            HStack {
                                Text(selectedArtistMap.isEmpty ? "Continue" : "Done (\(selectedArtistMap.count) selected)")
                                    .font(.system(size: 16, weight: .semibold))
                            }
                            .foregroundColor(.black)
                            .frame(maxWidth: .infinity)
                            .frame(height: 50)
                            .background(Color.white)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                        }

                        Button(action: skipAndDismiss) {
                            Text("Skip for now")
                                .font(.system(size: 14))
                                .foregroundColor(.white.opacity(0.55))
                                .padding(.vertical, 4)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 10)
                    .padding(.bottom, 16)
                    .background(
                        LinearGradient(
                            colors: [Color.black.opacity(0.95), Color.black],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        saveAndDismiss()
                    }
                    .foregroundColor(.white)
                    .fontWeight(.semibold)
                }
            }
            .onAppear {
                // Initialize selection with existing favorites
                for artist in viewModel.favoriteArtists {
                    selectedArtistMap[artistKey(artist)] = artist
                }
            }
        }
    }

    // Combine existing favorite artists with popular curated list to make sure already selected artists appear
    private var displayedPopularArtists: [Artist] {
        var list = popularArtists
        for fav in viewModel.favoriteArtists {
            let key = artistKey(fav)
            if !list.contains(where: { artistKey($0) == key }) {
                list.insert(fav, at: 0)
            }
        }
        return list
    }

    private func artistKey(_ artist: Artist) -> String {
        if !artist.id.isEmpty { return artist.id }
        return artist.name.lowercased()
    }

    private func isSelected(_ artist: Artist) -> Bool {
        return selectedArtistMap[artistKey(artist)] != nil
    }

    private func toggleSelection(_ artist: Artist) {
        let key = artistKey(artist)
        if selectedArtistMap[key] != nil {
            selectedArtistMap.removeValue(forKey: key)
        } else {
            selectedArtistMap[key] = artist
        }
    }

    private func artistCard(_ artist: Artist) -> some View {
        let selected = isSelected(artist)
        return Button(action: {
            toggleSelection(artist)
        }) {
            VStack(spacing: 8) {
                ZStack(alignment: .topTrailing) {
                    ArtistPhotoView(artist: artist)
                        .frame(width: 90, height: 90)
                        .clipShape(Circle())
                        .overlay(
                            Circle()
                                .stroke(selected ? Color.white : Color.white.opacity(0.1), lineWidth: selected ? 3 : 1)
                        )
                        .shadow(color: selected ? Color.white.opacity(0.3) : .clear, radius: 8)

                    if selected {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 22))
                            .foregroundColor(.white)
                            .background(Circle().fill(Color.black))
                            .offset(x: 2, y: -2)
                    }
                }

                Text(artist.name)
                    .font(.system(size: 13, weight: selected ? .semibold : .regular))
                    .foregroundColor(selected ? .white : .white.opacity(0.85))
                    .lineLimit(2)
                    .multilineTextAlignment(.center)
                    .frame(width: 96, height: 34, alignment: .top)
            }
        }
        .buttonStyle(.plain)
    }

    private func triggerSearch(query: String) {
        searchTask?.cancel()
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            viewModel.searchedArtists = []
            return
        }

        searchTask = Task {
            try? await Task.sleep(nanoseconds: 350_000_000)
            guard !Task.isCancelled else { return }
            await viewModel.searchArtists(query: trimmed)
        }
    }

    private func saveAndDismiss() {
        let artists = Array(selectedArtistMap.values)
        viewModel.saveFavoriteArtists(artists)
        viewModel.showFavoriteArtistsPrompt = false
        dismiss()
    }

    private func skipAndDismiss() {
        UserDefaults.standard.set(true, forKey: "musesick_has_selected_favorites")
        viewModel.showFavoriteArtistsPrompt = false
        dismiss()
    }

    private func artistGradient(for name: String) -> LinearGradient {
        let palettes: [[Color]] = [
            [Color.purple.opacity(0.8), Color.blue.opacity(0.8)],
            [Color.pink.opacity(0.8), Color.orange.opacity(0.8)],
            [Color.teal.opacity(0.8), Color.indigo.opacity(0.8)],
            [Color.red.opacity(0.8), Color.purple.opacity(0.8)],
            [Color.blue.opacity(0.8), Color.cyan.opacity(0.8)],
            [Color.orange.opacity(0.8), Color.red.opacity(0.8)]
        ]
        let index = abs(name.hashValue) % palettes.count
        return LinearGradient(
            colors: palettes[index],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }

    private func artistInitials(for name: String) -> String {
        let parts = name.split(separator: " ").filter { !$0.isEmpty }
        if parts.count >= 2 {
            return "\(parts[0].prefix(1))\(parts[1].prefix(1))".uppercased()
        } else if let first = parts.first {
            return String(first.prefix(2)).uppercased()
        }
        return "♪"
    }
}
