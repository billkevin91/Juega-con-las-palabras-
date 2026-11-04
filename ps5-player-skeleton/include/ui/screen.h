#pragma once
#include "input/gamepad.h"
#include "ui/renderer.h"

namespace ui {

class Screen {
public:
    virtual ~Screen() = default;
    virtual void onInput(const input::GamepadEvent& ev) = 0;
    virtual void update(float dt) = 0;
    virtual void draw(Renderer& r) = 0;

    virtual Screen* nextScreen() { Screen* n = next_; next_ = nullptr; return n; }
    virtual bool    shouldQuit() const { return quit_; }

protected:
    Screen* next_ = nullptr;
    bool    quit_ = false;
};

} // namespace ui
