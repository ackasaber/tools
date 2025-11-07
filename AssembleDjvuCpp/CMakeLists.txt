cmake_minimum_required(VERSION 4.0)
project(AssembleDjvuCpp)

add_subdirectory(libdjvu)

find_package(fmt 12 REQUIRED)

add_executable(AssembleDjvuCpp main.cpp)
set_target_properties(AssembleDjvuCpp PROPERTIES
        CXX_STANDARD 23
        CXX_STANDARD_REQUIRED ON
        CXX_EXTENSIONS OFF
        OUTPUT_NAME "assemble-djvu")
target_compile_options(AssembleDjvuCpp PRIVATE
        $<$<CXX_COMPILER_ID:MSVC>:/W4 /WX>
        $<$<NOT:$<CXX_COMPILER_ID:MSVC>>:-Wall -Wextra -Wpedantic -Wno-unused-parameter -Werror>)

target_link_libraries(AssembleDjvuCpp PRIVATE djvulibre fmt::fmt)
