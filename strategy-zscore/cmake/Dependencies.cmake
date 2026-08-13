include(FetchContent)

set(AUTOTRADER_CPP_HTTPLIB_VERSION "0.53.0")
set(AUTOTRADER_NLOHMANN_JSON_VERSION "3.12.0")
set(AUTOTRADER_GOOGLETEST_VERSION "1.18.0")

function(autotrader_strategy_fetch_runtime_dependencies)
    FetchContent_Declare(
        cpp_httplib
        GIT_REPOSITORY https://github.com/yhirose/cpp-httplib.git
        GIT_TAG v${AUTOTRADER_CPP_HTTPLIB_VERSION}
        GIT_SHALLOW TRUE
    )

    FetchContent_Declare(
        nlohmann_json
        GIT_REPOSITORY https://github.com/nlohmann/json.git
        GIT_TAG v${AUTOTRADER_NLOHMANN_JSON_VERSION}
        GIT_SHALLOW TRUE
    )

    FetchContent_MakeAvailable(cpp_httplib nlohmann_json)
endfunction()

function(autotrader_strategy_fetch_test_dependencies)
    if(WIN32)
        set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)
    endif()

    FetchContent_Declare(
        googletest
        GIT_REPOSITORY https://github.com/google/googletest.git
        GIT_TAG v${AUTOTRADER_GOOGLETEST_VERSION}
        GIT_SHALLOW TRUE
    )

    FetchContent_MakeAvailable(googletest)
endfunction()
