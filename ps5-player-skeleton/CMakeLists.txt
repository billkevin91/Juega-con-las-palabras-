cmake_minimum_required(VERSION 3.20)
project(ps5_player LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_compile_options(-Wall -Wextra -Wpedantic -fno-rtti -fno-exceptions)
add_compile_definitions(__PS5__=1)

# ---- third-party (expected as submodules, or provided by the SDK sysroot) ----
find_package(PkgConfig REQUIRED)
pkg_check_modules(SDL2   REQUIRED IMPORTED_TARGET sdl2)
pkg_check_modules(CURL   REQUIRED IMPORTED_TARGET libcurl)
pkg_check_modules(AVFMT  REQUIRED IMPORTED_TARGET libavformat)
pkg_check_modules(AVCDC  REQUIRED IMPORTED_TARGET libavcodec)
pkg_check_modules(AVUTIL REQUIRED IMPORTED_TARGET libavutil)
pkg_check_modules(SWS    REQUIRED IMPORTED_TARGET libswscale)
pkg_check_modules(SWR    REQUIRED IMPORTED_TARGET libswresample)

file(GLOB_RECURSE SRC CONFIGURE_DEPENDS src/*.cpp src/*.c)

add_executable(ps5_player ${SRC})
target_include_directories(ps5_player PRIVATE include)
target_link_libraries(ps5_player PRIVATE
    PkgConfig::SDL2
    PkgConfig::CURL
    PkgConfig::AVFMT
    PkgConfig::AVCDC
    PkgConfig::AVUTIL
    PkgConfig::SWS
    PkgConfig::SWR
)
