#pragma once
#include "core/config.h"
#include "ui/screen.h"

namespace ui {

class HomeScreen : public Screen {
public:
    explicit HomeScreen(core::Config& cfg) : cfg_(cfg) {}
    void onInput(const input::GamepadEvent& ev) override;
    void update(float dt) override;
    void draw(Renderer& r) override;

private:
    core::Config& cfg_;
    size_t cursor_ = 0;
};

} // namespace ui
