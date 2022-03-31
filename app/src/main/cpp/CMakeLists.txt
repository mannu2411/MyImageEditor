cmake_minimum_required(VERSION 3.6)

add library(photoEditor SHARED photoEditor.c)

target_link_libraries(photoEditor android)