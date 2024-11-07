#pragma once

#include "MainWindow.g.h"

namespace winrt::AssembleDjvu::implementation
{
    struct MainWindow : MainWindowT<MainWindow>
    {
        MainWindow();

        void ExecuteAddCommand(winrt::Microsoft::UI::Xaml::Input::XamlUICommand const& sender,
            winrt::Microsoft::UI::Xaml::Input::ExecuteRequestedEventArgs const& args);

    };
}

namespace winrt::AssembleDjvu::factory_implementation
{
    struct MainWindow : MainWindowT<MainWindow, implementation::MainWindow>
    {
    };
}
