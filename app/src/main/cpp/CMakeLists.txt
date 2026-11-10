cmake_minimum_required(VERSION 3.22.1)
project(bpg_android LANGUAGES C CXX)

set(CMAKE_C_STANDARD 99)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# x265 is built as a static library for each Android ABI.
set(ENABLE_SHARED OFF CACHE BOOL "" FORCE)
set(ENABLE_ASSEMBLY OFF CACHE BOOL "" FORCE)
set(ENABLE_PIC ON CACHE BOOL "" FORCE)
set(ENABLE_LIBNUMA OFF CACHE BOOL "" FORCE)
set(NATIVE_BUILD OFF CACHE BOOL "" FORCE)
set(CHECKED_BUILD OFF CACHE BOOL "" FORCE)
set(ENABLE_PPA OFF CACHE BOOL "" FORCE)
set(ENABLE_VTUNE OFF CACHE BOOL "" FORCE)
set(ENABLE_CLI OFF CACHE BOOL "" FORCE)

set(X265_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/third_party/x265/source")
add_subdirectory("${X265_SOURCE_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/x265")

add_library(bpg SHARED
    bpgenc_android.c
    third_party/libbpg/bpgenc.h
    x265_glue.c
)

target_include_directories(bpg PRIVATE
    "${CMAKE_CURRENT_SOURCE_DIR}/third_party/libbpg"
    "${X265_SOURCE_DIR}"
    "${CMAKE_CURRENT_BINARY_DIR}/x265"
)

target_compile_definitions(bpg PRIVATE
    USE_X265
    _ISOC99_SOURCE
    _POSIX_C_SOURCE=200112L
    _XOPEN_SOURCE=600
    _GNU_SOURCE=1
    _FILE_OFFSET_BITS=64
    _LARGEFILE_SOURCE
    _REENTRANT
)

target_compile_options(bpg PRIVATE
    -O2
    -fno-strict-aliasing
    -fvisibility=hidden
)

target_link_libraries(bpg PRIVATE
    x265-static
    m
    dl
)
if(NOT ANDROID)
    target_link_libraries(bpg PRIVATE pthread)
endif()

# The x265 static archive contains C++ objects; use the C++ driver for final linking.
set_target_properties(bpg PROPERTIES LINKER_LANGUAGE CXX)
