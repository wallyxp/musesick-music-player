import SwiftUI

public struct SettingsView: View {
    @ObservedObject var viewModel: MusicViewModel

    public init(viewModel: MusicViewModel) {
        self.viewModel = viewModel
    }

    public var body: some View {
        NavigationStack {
            ZStack {
                Color.black.ignoresSafeArea()

                ScrollView {
                    VStack(alignment: .leading, spacing: 24) {
                        // Section: Appearance
                        VStack(alignment: .leading, spacing: 10) {
                            Text("APPEARANCE")
                                .font(.system(size: 13, weight: .bold))
                                .tracking(1.2)
                                .foregroundColor(.white.opacity(0.7))
                                .padding(.horizontal, 16)

                            VStack(spacing: 1) {
                                HStack {
                                    Label("App Theme", systemImage: "paintpalette.fill")
                                        .foregroundColor(.white)
                                    Spacer()
                                    Text("AMOLED Dark")
                                        .foregroundColor(.white.opacity(0.6))
                                }
                                .padding(16)
                                .background(Color(white: 0.12))
                            }
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                            .padding(.horizontal, 16)
                        }

                        // Section: About
                        VStack(alignment: .leading, spacing: 10) {
                            Text("ABOUT")
                                .font(.system(size: 13, weight: .bold))
                                .tracking(1.2)
                                .foregroundColor(.white.opacity(0.7))
                                .padding(.horizontal, 16)

                            VStack(spacing: 1) {
                                HStack {
                                    Text("Application")
                                        .foregroundColor(.white)
                                    Spacer()
                                    Text("Musesick Music Player")
                                        .foregroundColor(.white.opacity(0.6))
                                }
                                .padding(16)
                                .background(Color(white: 0.12))

                                Divider().background(Color.white.opacity(0.08))

                                HStack {
                                    Text("Version")
                                        .foregroundColor(.white)
                                    Spacer()
                                    Text(viewModel.appVersion)
                                        .foregroundColor(.white.opacity(0.6))
                                }
                                .padding(16)
                                .background(Color(white: 0.12))

                                Divider().background(Color.white.opacity(0.08))

                                HStack {
                                    Text("Platform")
                                        .foregroundColor(.white)
                                    Spacer()
                                    Text("iOS Native")
                                        .foregroundColor(.white.opacity(0.6))
                                }
                                .padding(16)
                                .background(Color(white: 0.12))
                            }
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                            .padding(.horizontal, 16)
                        }

                        Spacer().frame(height: 80)
                    }
                    .padding(.top, 10)
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.large)
            .toolbarBackground(Color.black, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
        }
    }
}
