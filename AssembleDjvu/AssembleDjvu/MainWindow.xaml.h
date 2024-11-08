#pragma once

#include "MainWindow.g.h"

namespace winrt::AssembleDjvu::implementation
{
    struct MainWindow : MainWindowT<MainWindow>
    {
        using XamlUICommand = winrt::Microsoft::UI::Xaml::Input::XamlUICommand;
        using ExecuteRequestedEventArgs = winrt::Microsoft::UI::Xaml::Input::ExecuteRequestedEventArgs;

        MainWindow();

        void ExecuteAddCommand(XamlUICommand const& sender, ExecuteRequestedEventArgs const& args);
        void ExecuteConvertCommand(XamlUICommand const& sender, ExecuteRequestedEventArgs const& args);
    };
}

namespace winrt::AssembleDjvu::factory_implementation
{
    struct MainWindow : MainWindowT<MainWindow, implementation::MainWindow>
    {
    };
}
