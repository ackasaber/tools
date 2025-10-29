cmake_minimum_required(VERSION 4.0)
project(AssembleDjvuCpp)

add_subdirectory(libdjvu)

add_executable(AssembleDjvuCpp main.cpp)
target_compile_features(AssembleDjvuCpp PRIVATE cxx_std_23)
set_target_properties(AssembleDjvuCpp PROPERTIES CXX_EXTENSIONS OFF)
target_compile_options(AssembleDjvuCpp PRIVATE
        $<$<CXX_COMPILER_ID:MSVC>:/W4 /WX>
        $<$<NOT:$<CXX_COMPILER_ID:MSVC>>:-Wall -Wextra -Wpedantic -Werror>)

find_package(JPEG)
target_link_libraries(AssembleDjvuCpp PRIVATE JPEG::JPEG djvulibre)
