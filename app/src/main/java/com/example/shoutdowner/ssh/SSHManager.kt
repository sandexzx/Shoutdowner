package com.example.shoutdowner.ssh

import com.example.shoutdowner.data.SshSettings
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*

sealed class ExecutionResult {
    data class Success(val output: String) : ExecutionResult()
    data class Error(val message: String) : ExecutionResult()
}

class SSHManager {

    /**
     * Выполняет команду на удалённой машине по SSH.
     * Использует password или privateKeyPem из SshSettings.
     *
     * Внимание: для простоты тут отключена проверка known_hosts (StrictHostKeyChecking=no).
     */
    suspend fun executeCommand(
        settings: SshSettings,
        command: String,
        timeoutMs: Int = 15_000
    ): ExecutionResult = withContext(Dispatchers.IO) {
        try {
            val jsch = JSch()

            // If a PEM private key is provided, try to add it as identity.
            if (!settings.usePassword && settings.privateKeyPem.isNotBlank()) {
                // JSch supports adding identity by byte array
                val keyBytes = settings.privateKeyPem.toByteArray(Charset.forName("UTF-8"))
                try {
                    jsch.addIdentity("key-${UUID.randomUUID()}", keyBytes, null, null)
                } catch (e: Exception) {
                    // fallback: write key to a temp file and add by path (avoids ambiguous overloads)
                    try {
                        val tempFile = java.io.File.createTempFile("sshkey", ".pem")
                        tempFile.writeText(settings.privateKeyPem, Charset.forName("UTF-8"))
                        jsch.addIdentity(tempFile.absolutePath)
                        tempFile.delete()
                    } catch (ex: Exception) {
                        return@withContext ExecutionResult.Error("Ошибка добавления приватного ключа: ${ex.message}")
                    }
                }
            }

            val session = jsch.getSession(settings.username, settings.host, settings.port)
            if (settings.usePassword) {
                session.setPassword(settings.password)
            }

            val config = Properties()
            config["StrictHostKeyChecking"] = "no"
            session.setConfig(config)

            session.connect(timeoutMs)

            val channel = session.openChannel("exec") as ChannelExec
            channel.setCommand(command)
            channel.setInputStream(null)

            val stdout = channel.inputStream
            val stderrStream = ByteArrayOutputStream()
            channel.setErrStream(stderrStream)

            channel.connect(timeoutMs / 2)

            val output = readStreamFully(stdout, channel)
            // wait until channel closed
            while (!channel.isClosed) {
                Thread.sleep(50)
            }

            val exitStatus = channel.exitStatus
            val errString = stderrStream.toString("UTF-8").trim()

            channel.disconnect()
            session.disconnect()

            return@withContext if (exitStatus == 0) {
                ExecutionResult.Success(output.trim())
            } else {
                val msg = if (errString.isNotEmpty()) errString else "Команда вернула статус $exitStatus. Выход: $output"
                ExecutionResult.Error(msg)
            }
        } catch (e: Exception) {
            return@withContext ExecutionResult.Error("SSH ошибка: ${e.message}")
        }
    }

    private fun readStreamFully(input: InputStream, channel: ChannelExec): String {
        val buf = ByteArray(1024)
        val out = StringBuilder()
        try {
            while (true) {
                while (input.available() > 0) {
                    val i = input.read(buf, 0, buf.size)
                    if (i < 0) break
                    out.append(String(buf, 0, i, Charset.forName("UTF-8")))
                }
                if (channel.isClosed) {
                    if (input.available() > 0) continue
                    break
                }
                try {
                    Thread.sleep(50)
                } catch (_: InterruptedException) {
                }
            }
        } catch (e: Exception) {
            // ignore read errors here, they will be handled by caller
        }
        return out.toString()
    }
}
