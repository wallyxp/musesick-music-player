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

        switch event {
        case "onReady":
            NSLog("[YouTubeBridge] Player reported ready")
            delegate?.onPlayerReady()
        case "onStateChange":
            if let state = dict["data"] as? Int {
                NSLog("[YouTubeBridge] State changed: %d", state)
                delegate?.onStateChange(state: state)
            }
        case "onTimeUpdate":
            let curr = dict["currentTime"] as? Double ?? 0.0
            let dur = dict["duration"] as? Double ?? 0.0
            delegate?.onTimeUpdate(currentTimeSeconds: curr, durationSeconds: dur)
        case "onError":
            let code = dict["data"] as? Int ?? -1
            NSLog("[YouTubeBridge] Player error: %d", code)
            delegate?.onError(errorCode: code)
        case "console":
            if let msg = dict["message"] as? String {
                NSLog("[YouTube JS] %@", msg)
            }
        case "consoleError":
            if let msg = dict["message"] as? String {
                NSLog("[YouTube JS Error] %@", msg)
            }
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
        // Desktop Safari UA avoids mobile user-interaction blocks for embedded playback
        wv.customUserAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Safari/605.1.15"
        if #available(iOS 16.4, *) {
            wv.isInspectable = true
        }
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
            <meta http-equiv="Content-Security-Policy" content="default-src * 'unsafe-inline' 'unsafe-eval' data: blob:;">
            <style>
                html, body {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    height: 100%;
                    background-color: #000;
                    overflow: hidden;
                }
                #player {
                    position: absolute;
                    top: 0;
                    left: 0;
                    width: 100%;
                    height: 100%;
                }
            </style>
        </head>
        <body>
            <div id="player"></div>
            <script>
                var player = null;
                var isPlayerReady = false;
                var pendingId = null;

                function sendMsg(obj) {
                    try {
                        if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.iOSBridge) {
                            window.webkit.messageHandlers.iOSBridge.postMessage(obj);
                        }
                    } catch(e) {}
                }

                function log(msg) {
                    sendMsg({event: 'console', message: String(msg)});
                }

                function logError(msg) {
                    sendMsg({event: 'consoleError', message: String(msg)});
                }

                var observer = new MutationObserver(function(mutations) {
                    var iframes = document.querySelectorAll('iframe');
                    iframes.forEach(function(iframe) {
                        iframe.setAttribute('allow', 'autoplay; encrypted-media; picture-in-picture');
                        iframe.setAttribute('playsinline', '1');
                    });
                });
                observer.observe(document.body, { childList: true, subtree: true });

                var tag = document.createElement('script');
                tag.src = "https://www.youtube.com/iframe_api";
                var firstScriptTag = document.getElementsByTagName('script')[0];
                firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                function onYouTubeIframeAPIReady() {
                    log('onYouTubeIframeAPIReady called');
                    try {
                        player = new YT.Player('player', {
                            height: '100%',
                            width: '100%',
                            playerVars: {
                                'playsinline': 1,
                                'controls': 0,
                                'autoplay': 1,
                                'rel': 0,
                                'origin': 'https://music.youtube.com'
                            },
                            events: {
                                'onReady': onPlayerReady,
                                'onStateChange': onPlayerStateChange,
                                'onError': onPlayerError
                            }
                        });
                    } catch(e) {
                        logError('Error creating YT.Player: ' + e);
                    }
                }

                function onPlayerReady(event) {
                    log('onPlayerReady fired');
                    isPlayerReady = true;
                    sendMsg({event: 'onReady'});
                    if (pendingId) {
                        var vid = pendingId;
                        pendingId = null;
                        playVideo(vid);
                    }
                }

                function onPlayerStateChange(event) {
                    log('onPlayerStateChange: ' + event.data);
                    sendMsg({event: 'onStateChange', data: event.data});
                }

                function onPlayerError(event) {
                    logError('onPlayerError: ' + event.data);
                    sendMsg({event: 'onError', data: event.data});
                }

                function playVideo(id) {
                    log('playVideo called for: ' + id + ', isPlayerReady: ' + isPlayerReady);
                    if (!isPlayerReady || !player || !player.loadVideoById) {
                        pendingId = id;
                        return;
                    }
                    try {
                        player.loadVideoById(id);
                        setTimeout(function() {
                            if (player && player.playVideo) {
                                player.playVideo();
                            }
                        }, 100);
                    } catch(e) {
                        logError('playVideo error: ' + e);
                    }
                }

                function pauseVideo() {
                    if (player && player.pauseVideo) {
                        player.pauseVideo();
                    }
                }

                function resumeVideo() {
                    if (player && player.playVideo) {
                        player.playVideo();
                    }
                }

                function stopVideo() {
                    if (player && player.stopVideo) {
                        player.stopVideo();
                    }
                }

                function seekTo(sec) {
                    if (player && player.seekTo) {
                        player.seekTo(sec, true);
                    }
                }

                setInterval(function() {
                    if (player && player.getCurrentTime && player.getDuration) {
                        try {
                            var curr = player.getCurrentTime();
                            var dur = player.getDuration();
                            if (dur > 0) {
                                sendMsg({
                                    event: 'onTimeUpdate',
                                    currentTime: curr,
                                    duration: dur
                                });
                            }
                        } catch(e) {}
                    }
                }, 500);
            </script>
        </body>
        </html>
        """

        webView.loadHTMLString(html, baseURL: URL(string: "https://music.youtube.com"))
    }

    public func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        NSLog("[YouTubeWebEngine] HTML skeleton finished loading in WKWebView")
        // Note: isReady is set when onPlayerReady message is received from YouTube API
    }

    public func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        NSLog("[YouTubeWebEngine] Provisional navigation failed: %@", error.localizedDescription)
    }

    public func markReady() {
        NSLog("[YouTubeWebEngine] Player is ready to accept commands")
        isReady = true
        if let pending = pendingVideoId {
            NSLog("[YouTubeWebEngine] Executing pending playback for videoId: %@", pending)
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
