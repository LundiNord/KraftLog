package de.nyxnord.kraftlog.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import de.nyxnord.kraftlog.data.ShareableStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.util.UUID

class BluetoothShareService(private val adapter: BluetoothAdapter) {

    private val appUuid = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
    private val serviceName = "KraftLogShare"

    @Volatile private var pendingServerSocket: BluetoothServerSocket? = null

    fun cancel() {
        pendingServerSocket?.close()
    }

    @SuppressLint("MissingPermission")
    suspend fun startServer(myStats: ShareableStats): ShareableStats = withContext(Dispatchers.IO) {
        val serverSocket = adapter.listenUsingRfcommWithServiceRecord(serviceName, appUuid)
        pendingServerSocket = serverSocket
        try {
            val clientSocket = serverSocket.accept()
            serverSocket.close()
            pendingServerSocket = null
            exchange(clientSocket, myStats)
        } finally {
            pendingServerSocket = null
            runCatching { serverSocket.close() }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun connectToDevice(device: BluetoothDevice, myStats: ShareableStats): ShareableStats =
        withContext(Dispatchers.IO) {
            val socket = device.createRfcommSocketToServiceRecord(appUuid)
            try {
                adapter.cancelDiscovery()
                socket.connect()
                exchange(socket, myStats)
            } finally {
                runCatching { socket.close() }
            }
        }

    private suspend fun exchange(socket: BluetoothSocket, myStats: ShareableStats): ShareableStats =
        coroutineScope {
            val readDeferred = async(Dispatchers.IO) {
                BufferedReader(InputStreamReader(socket.inputStream)).readLine()
            }
            // Write on the current thread after starting the read
            PrintWriter(socket.outputStream, true).println(myStats.toJson())
            ShareableStats.fromJson(readDeferred.await())
        }
}
