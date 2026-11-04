#include "input/gamepad.h"

#include <SDL2/SDL.h>

namespace input {

static Button mapSDL(SDL_GameControllerButton b) {
    switch (b) {
        case SDL_CONTROLLER_BUTTON_A:             return Button::Cross;
        case SDL_CONTROLLER_BUTTON_B:             return Button::Circle;
        case SDL_CONTROLLER_BUTTON_X:             return Button::Square;
        case SDL_CONTROLLER_BUTTON_Y:             return Button::Triangle;
        case SDL_CONTROLLER_BUTTON_DPAD_UP:       return Button::DUp;
        case SDL_CONTROLLER_BUTTON_DPAD_DOWN:     return Button::DDown;
        case SDL_CONTROLLER_BUTTON_DPAD_LEFT:     return Button::DLeft;
        case SDL_CONTROLLER_BUTTON_DPAD_RIGHT:    return Button::DRight;
        case SDL_CONTROLLER_BUTTON_LEFTSHOULDER:  return Button::L1;
        case SDL_CONTROLLER_BUTTON_RIGHTSHOULDER: return Button::R1;
        case SDL_CONTROLLER_BUTTON_START:         return Button::Options;
        case SDL_CONTROLLER_BUTTON_BACK:          return Button::Share;
        case SDL_CONTROLLER_BUTTON_GUIDE:         return Button::PSBtn;
        case SDL_CONTROLLER_BUTTON_LEFTSTICK:     return Button::L3;
        case SDL_CONTROLLER_BUTTON_RIGHTSTICK:    return Button::R3;
        default:                                  return Button::Cross;
    }
}

GamepadManager::GamepadManager() {
    SDL_InitSubSystem(SDL_INIT_GAMECONTROLLER);
    for (int i = 0; i < SDL_NumJoysticks(); ++i) {
        if (SDL_IsGameController(i)) SDL_GameControllerOpen(i);
    }
}
GamepadManager::~GamepadManager() {
    SDL_QuitSubSystem(SDL_INIT_GAMECONTROLLER);
}

void GamepadManager::poll() {
    events_.clear();
    SDL_Event e;
    while (SDL_PollEvent(&e)) {
        if (e.type == SDL_CONTROLLERBUTTONDOWN || e.type == SDL_CONTROLLERBUTTONUP) {
            GamepadEvent ev;
            ev.pad     = e.cbutton.which & 3;
            ev.btn     = mapSDL(static_cast<SDL_GameControllerButton>(e.cbutton.button));
            ev.pressed = (e.type == SDL_CONTROLLERBUTTONDOWN);
            events_.push_back(ev);
        } else if (e.type == SDL_CONTROLLERAXISMOTION) {
            int p = e.caxis.which & 3;
            float v = e.caxis.value / 32767.0f;
            switch (e.caxis.axis) {
                case SDL_CONTROLLER_AXIS_LEFTX:  sticks_[p].lx = v; break;
                case SDL_CONTROLLER_AXIS_LEFTY:  sticks_[p].ly = v; break;
                case SDL_CONTROLLER_AXIS_RIGHTX: sticks_[p].rx = v; break;
                case SDL_CONTROLLER_AXIS_RIGHTY: sticks_[p].ry = v; break;
                default: break;
            }
        }
    }
}

} // namespace input
