#include "ui/home_screen.h"

namespace ui {

void HomeScreen::onInput(const input::GamepadEvent& ev) {
    if (!ev.pressed) return;
    const auto& srcs = cfg_.sources();
    switch (ev.btn) {
        case input::Button::DUp:    if (cursor_ > 0) --cursor_; break;
        case input::Button::DDown:  if (cursor_ + 1 < srcs.size()) ++cursor_; break;
        case input::Button::Circle: quit_ = true; break;
        case input::Button::Cross:  /* TODO: push SourceScreen(srcs[cursor_]) */ break;
        case input::Button::Options:/* TODO: push AddSourceScreen(cfg_) */ break;
        default: break;
    }
}

void HomeScreen::update(float /*dt*/) {}

void HomeScreen::draw(Renderer& r) {
    r.drawRect(0, 0, (float)r.width(), (float)r.height(), 0x101418FFu);
    r.drawText(80, 60, "Sources", 0xFFFFFFFFu, 48);

    const auto& srcs = cfg_.sources();
    if (srcs.empty()) {
        r.drawText(80, 180, "No sources yet. Press Options to add one.",
                   0xBBBBBBFFu, 24);
        return;
    }
    float y = 180;
    for (size_t i = 0; i < srcs.size(); ++i) {
        uint32_t col = (i == cursor_) ? 0xFFEE88FFu : 0xDDDDDDFFu;
        r.drawText(80, y, srcs[i].name, col, 28);
        y += 42;
    }
}

} // namespace ui
