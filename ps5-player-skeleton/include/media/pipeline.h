#pragma once
#include <cstdint>
#include <string>

extern "C" {
struct AVFormatContext;
struct AVCodecContext;
struct AVFrame;
struct AVPacket;
}

namespace media {

struct VideoInfo { int width = 0; int height = 0; int fps_num = 0; int fps_den = 1; };
struct AudioInfo { int sample_rate = 0; int channels = 0; };

class MediaPipeline {
public:
    MediaPipeline();
    ~MediaPipeline();
    MediaPipeline(const MediaPipeline&) = delete;
    MediaPipeline& operator=(const MediaPipeline&) = delete;

    bool open(const std::string& url);
    void close();

    bool readVideoFrame(AVFrame** out);
    bool readAudioFrame(AVFrame** out);

    const VideoInfo& videoInfo() const { return video_; }
    const AudioInfo& audioInfo() const { return audio_; }

private:
    AVFormatContext* fmt_ = nullptr;
    AVCodecContext*  vcodec_ = nullptr;
    AVCodecContext*  acodec_ = nullptr;
    AVPacket*        pkt_ = nullptr;
    int videoIdx_ = -1;
    int audioIdx_ = -1;
    VideoInfo video_;
    AudioInfo audio_;
};

} // namespace media
