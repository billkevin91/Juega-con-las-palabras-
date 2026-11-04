# Toolchain file for the open PS5 homebrew SDK.
# Adjust PS5SDK to your local install; the triple matches recent PS5SDK builds.
set(CMAKE_SYSTEM_NAME Generic)
set(CMAKE_SYSTEM_PROCESSOR x86_64)

if(NOT DEFINED ENV{PS5SDK})
    message(FATAL_ERROR "PS5SDK env var is not set")
endif()
set(PS5SDK $ENV{PS5SDK})

set(CMAKE_C_COMPILER   "${PS5SDK}/bin/clang")
set(CMAKE_CXX_COMPILER "${PS5SDK}/bin/clang++")
set(CMAKE_AR           "${PS5SDK}/bin/llvm-ar"     CACHE FILEPATH "" FORCE)
set(CMAKE_RANLIB       "${PS5SDK}/bin/llvm-ranlib" CACHE FILEPATH "" FORCE)

set(TARGET_TRIPLE "x86_64-sie-ps5")
add_compile_options(--target=${TARGET_TRIPLE} -fPIE -ffunction-sections -fdata-sections)
add_link_options(--target=${TARGET_TRIPLE} -pie -Wl,--gc-sections
                 -T "${PS5SDK}/link/elf.ld")

set(CMAKE_SYSROOT        "${PS5SDK}/sysroot")
set(CMAKE_FIND_ROOT_PATH "${PS5SDK}/sysroot")
set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_PACKAGE ONLY)
