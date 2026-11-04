#include "media/pipeline.h"
#include "core/logger.h"

extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
}

namespace media {

MediaPipeline::MediaPipeline() { pkt_ = av_packet_alloc(); }
MediaPipeline::~MediaPipeline() { close(); if (pkt_) av_packet_free(&pkt_); }

static AVCodecContext* openCodec(AVFormatContext* fmt, int idx) {
    const AVCodec* codec = avcodec_find_decoder(fmt->streams[idx]->codecpar->codec_id);
    if (!codec) return nullptr;
    AVCodecContext* ctx = avcodec_alloc_context3(codec);
    if (!ctx) return nullptr;
    avcodec_parameters_to_context(ctx, fmt->streams[idx]->codecpar);
    if (avcodec_open2(ctx, codec, nullptr) < 0) { avcodec_free_context(&ctx); return nullptr; }
    return ctx;
}

bool MediaPipeline::open(const std::string& url) {
    close();
    if (avformat_open_input(&fmt_, url.c_str(), nullptr, nullptr) < 0) {
        LOGE("media: cannot open %s", url.c_str()); return false;
    }
    if (avformat_find_stream_info(fmt_, nullptr) < 0) return false;

    for (unsigned i = 0; i < fmt_->nb_streams; ++i) {
        auto* par = fmt_->streams[i]->codecpar;
        if (par->codec_type == AVMEDIA_TYPE_VIDEO && videoIdx_ < 0) videoIdx_ = i;
        if (par->codec_type == AVMEDIA_TYPE_AUDIO && audioIdx_ < 0) audioIdx_ = i;
    }
    if (videoIdx_ >= 0) {
        vcodec_ = openCodec(fmt_, videoIdx_);
        if (vcodec_) {
            video_.width  = vcodec_->width;
            video_.height = vcodec_->height;
            auto fr = fmt_->streams[videoIdx_]->r_frame_rate;
            video_.fps_num = fr.num; video_.fps_den = fr.den ? fr.den : 1;
        }
    }
    if (audioIdx_ >= 0) {
        acodec_ = openCodec(fmt_, audioIdx_);
        if (acodec_) {
            audio_.sample_rate = acodec_->sample_rate;
            audio_.channels    = acodec_->ch_layout.nb_channels;
        }
    }
    return vcodec_ || acodec_;
}

void MediaPipeline::close() {
    if (vcodec_) avcodec_free_context(&vcodec_);
    if (acodec_) avcodec_free_context(&acodec_);
    if (fmt_)    avformat_close_input(&fmt_);
    videoIdx_ = audioIdx_ = -1;
    video_ = {}; audio_ = {};
}

// Readers are intentionally thin stubs; the real pipeline enqueues frames to
// SPSC rings consumed by UI (video) and audio output threads.
bool MediaPipeline::readVideoFrame(AVFrame** out) { (void)out; return false; }
bool MediaPipeline::readAudioFrame(AVFrame** out) { (void)out; return false; }

} // namespace media
