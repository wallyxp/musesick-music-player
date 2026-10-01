import Foundation
import WebKit
import SwiftUI

public protocol YouTubePlayerDelegate: AnyObject {
    func onPlayerReady()
    func onStateChange(state: Int)
    func onTimeUpdate(currentTimeSeconds: Double, durationSeconds: Double)
    func onError(errorCode: Int)
}

public final class YouTubePlayerBridge: NSObject, WKScriptMessageHandler {
    public weak var delegate: YouTubePlayerDelegate?

    public func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        guard let dict = message.body as? [String: Any],
              let event = dict["event"] as? String else { return }

        #if DEBUG
        NSLog("[YouTubeBridge] Received event: %@, data: %@", event, String(describing: dict))
        #endif

        switch event {
        case "onReady":
            delegate?.onPlayerReady()
        case "onStateChange":
            if let state = dict["data"] as? Int {
                delegate?.onStateChange(state: state)
            }
        case "onTimeUpdate":
            let curr = dict["currentTime"] as? Double ?? 0.0
            let dur = dict["duration"] as? Double ?? 0.0
            delegate?.onTimeUpdate(currentTimeSeconds: curr, durationSeconds: dur)
        case "onError":
            let code = dict["data"] as? Int ?? -1
            delegate?.onError(errorCode: code)
        default:
            break
        }
    }
}

public final class YouTubeWebEngine: NSObject, WKNavigationDelegate {
    public static let shared = YouTubeWebEngine()

    public let webView: WKWebView
    private let bridge = YouTubePlayerBridge()
    private var isReady = false
    private var pendingVideoId: String?
    private var currentVideoId: String?

    public weak var delegate: YouTubePlayerDelegate? {
        didSet {
            bridge.delegate = delegate
        }
    }

    override private init() {
        let config = WKWebViewConfiguration()
        config.allowsInlineMediaPlayback = true
        config.mediaTypesRequiringUserActionForPlayback = []
        config.allowsAirPlayForMediaPlayback = true
        config.allowsPictureInPictureMediaPlayback = true
        config.defaultWebpagePreferences.allowsContentJavaScript = true

        let controller = WKUserContentController()
        controller.add(bridge, name: "iOSBridge")
        config.userContentController = controller

        let wv = WKWebView(frame: CGRect(x: 0, y: 0, width: 240, height: 240), configuration: config)
        wv.customUserAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"
        self.webView = wv

        super.init()
        wv.navigationDelegate = self
        loadPlayerHtml()
    }

    private func loadPlayerHtml() {
        let html = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                html, body {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    height: 100%;
                    background-color: #000;
                    overflow: hidden;
                }
                iframe {
                    width: 100%;
                    height: 100%;
                    border: 0;
                }
            </style>
        </head>
        <body>
            <iframe id="ytplayer"
                    type="text/html"
                    width="100%"
                    height="100%"
                    src="about:blank"
                    frameborder="0"
                    allow="autoplay; encrypted-media; picture-in-picture"
                    allowfullscreen>
            </iframe>
            <script>
                var tag = document.createElement('script');
                tag.src = "https://www.youtube.com/iframe_api";
                var firstScriptTag = document.getElementsByTagName('script')[0];
                firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                var player = null;
                var currentId = "";

                function onYouTubeIframeAPIReady() {
                    window.webkit.messageHandlers.iOSBridge.postMessage({event: 'onReady'});
                }

                function initPlayer(id) {
                    currentId = id;
                    var iframe = document.getElementById('ytplayer');
                    iframe.src = "https://www.youtube.com/embed/" + id + "?enablejsapi=1&autoplay=1&playsinline=1&controls=0&origin=https://www.youtube.com";

                    player = new YT.Player('ytplayer', {
                        events: {
                            'onReady': function(e) {
                                window.webkit.messageHandlers.iOSBridge.postMessage({event: 'onReady'});
                                if (player && player.playVideo) {
                                    player.playVideo();
                                }
                            },
                            'onStateChange': function(e) {
                                window.webkit.messageHandlers.iOSBridge.postMessage({event: 'onStateChange', data: e.data});
                            },
                            'onError': function(e) {
                                window.webkit.messageHandlers.iOSBridge.postMessage({event: 'onError', data: e.data});
                            }
                        }
                    });
                }

                function playVideo(id) {
                    if (player && player.loadVideoById && currentId !== "") {
                        currentId = id;
                        player.loadVideoById({
                            'videoId': id,
                            'startSeconds': 0
                        });
                        setTimeout(function() {
                            if (player && player.playVideo) {
                                player.playVideo();
                            }
                        }, 200);
                    } else if (typeof YT !== 'undefined' && YT.Player) {
                        initPlayer(id);
                    } else {
                        currentId = id;
                        var iframe = document.getElementById('ytplayer');
                        iframe.src = "https://www.youtube.com/embed/" + id + "?enablejsapi=1&autoplay=1&playsinline=1&controls=0&origin=https://www.youtube.com";
                    }
                }

                function pauseVideo() {
                    if (player && player.pauseVideo) {
                        player.pauseVideo();
                    } else {
                        var iframe = document.getElementById('ytplayer');
                        if (iframe && iframe.contentWindow) {
                            iframe.contentWindow.postMessage('{"event":"command","func":"pauseVideo","args":""}', '*');
                        }
                    }
                }

                function resumeVideo() {
                    if (player && player.playVideo) {
                        player.playVideo();
                    } else {
                        var iframe = document.getElementById('ytplayer');
                        if (iframe && iframe.contentWindow) {
                            iframe.contentWindow.postMessage('{"event":"command","func":"playVideo","args":""}', '*');
                        }
                    }
                }

                function stopVideo() {
                    if (player && player.stopVideo) {
                        player.stopVideo();
                    } else {
                        var iframe = document.getElementById('ytplayer');
                        if (iframe && iframe.contentWindow) {
                            iframe.contentWindow.postMessage('{"event":"command","func":"stopVideo","args":""}', '*');
                        }
                    }
                }

                function seekTo(sec) {
                    if (player && player.seekTo) {
                        player.seekTo(sec, true);
                    } else {
                        var iframe = document.getElementById('ytplayer');
                        if (iframe && iframe.contentWindow) {
                            iframe.contentWindow.postMessage('{"event":"command","func":"seekTo","args":[' + sec + ', true]}', '*');
                        }
                    }
                }

                window.addEventListener('message', function(event) {
                    try {
                        var data = (typeof event.data === 'string') ? JSON.parse(event.data) : event.data;
                        if (data && data.event === 'onStateChange') {
                            window.webkit.messageHandlers.iOSBridge.postMessage({event: 'onStateChange', data: data.info});
                        } else if (data && data.event === 'initialDelivery') {
                            window.webkit.messageHandlers.iOSBridge.postMessage({event: 'onReady'});
                        }
                    } catch(e) {}
                });

                setInterval(function() {
                    if (player && player.getCurrentTime && player.getDuration) {
                        var curr = player.getCurrentTime();
                        var dur = player.getDuration();
                        if (dur > 0) {
                            window.webkit.messageHandlers.iOSBridge.postMessage({
                                event: 'onTimeUpdate',
                                currentTime: curr,
                                duration: dur
                            });
                        }
                    }
                }, 500);
            </script>
        </body>
        </html>
        """

        webView.loadHTMLString(html, baseURL: URL(string: "https://www.youtube.com"))
    }

    public func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        #if DEBUG
        NSLog("[YouTubeWebEngine] HTML finished loading in WKWebView")
        #endif
        isReady = true
        if let pending = pendingVideoId {
            pendingVideoId = nil
            play(videoId: pending)
        }
    }

    public func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        NSLog("[YouTubeWebEngine] Provisional navigation failed: %@", error.localizedDescription)
    }

    public func markReady() {
        isReady = true
        if let pending = pendingVideoId {
            pendingVideoId = nil
            play(videoId: pending)
        }
    }

    public func play(videoId: String) {
        currentVideoId = videoId
        NSLog("[YouTubeWebEngine] play called with videoId: %@, isReady: %d", videoId, isReady ? 1 : 0)
        if isReady {
            webView.evaluateJavaScript("playVideo('\(videoId)');") { _, error in
                if let error = error {
                    NSLog("[YouTubeWebEngine] playVideo evaluate error: %@", error.localizedDescription)
                }
            }
        } else {
            pendingVideoId = videoId
        }
    }

    public func pause() {
        webView.evaluateJavaScript("pauseVideo();", completionHandler: nil)
    }

    public func resume() {
        webView.evaluateJavaScript("resumeVideo();", completionHandler: nil)
    }

    public func stop() {
        webView.evaluateJavaScript("stopVideo();", completionHandler: nil)
    }

    public func seek(toSeconds: Double) {
        webView.evaluateJavaScript("seekTo(\(toSeconds));", completionHandler: nil)
    }
}

public struct YouTubePlayerBackgroundView: UIViewRepresentable {
    public init() {}

    public func makeUIView(context: Context) -> WKWebView {
        return YouTubeWebEngine.shared.webView
    }

    public func updateUIView(_ uiView: WKWebView, context: Context) {}
}
