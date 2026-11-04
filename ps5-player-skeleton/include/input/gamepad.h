#pragma once
#include <vector>

namespace input {

enum class Button {
    Cross, Circle, Square, Triangle,
    DUp, DDown, DLeft, DRight,
    L1, R1, L2, R2,
    Options, Share, PSBtn, TouchpadClick,
    L3, R3,
};

struct GamepadEvent {
    int    pad = 0;
    Button btn;
    bool   pressed;
};

struct StickState {
    int   pad = 0;
    float lx = 0, ly = 0;
    float rx = 0, ry = 0;
};

class GamepadManager {
public:
    GamepadManager();
    ~GamepadManager();
    void poll();
    const std::vector<GamepadEvent>& events() const { return events_; }
    const StickState& sticks(int pad = 0) const { return sticks_[pad]; }

private:
    std::vector<GamepadEvent> events_;
    StickState sticks_[4]{};
};

} // namespace input
