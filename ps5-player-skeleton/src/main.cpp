#include "core/config.h"
#include "core/logger.h"
#include "input/gamepad.h"
#include "ui/home_screen.h"
#include "ui/renderer.h"

#include <memory>

int main(int /*argc*/, char** /*argv*/) {
    core::Logger::get().setLevel(core::LogLevel::Info);
    LOGI("ps5_player: boot");

    core::Config cfg = core::Config::load("/user/ps5_player/config.json");
    ui::Renderer renderer(1920, 1080);
    input::GamepadManager pads;

    std::unique_ptr<ui::Screen> current = std::make_unique<ui::HomeScreen>(cfg);

    while (!current->shouldQuit()) {
        pads.poll();
        for (const auto& ev : pads.events()) current->onInput(ev);

        current->update(renderer.dt());
        renderer.beginFrame();
        current->draw(renderer);
        renderer.endFrame();

        if (ui::Screen* n = current->nextScreen()) {
            current.reset(n);
        }
    }

    cfg.save("/user/ps5_player/config.json");
    return 0;
}
