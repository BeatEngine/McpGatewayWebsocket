$testImage = 'test-mcpsdemo-build:latest'
$containers = docker container ls -a --format '{{json .}}' | ConvertFrom-Json
foreach ($container in $containers)
{
    if($($container.Image) -match "^$testImage")
    {
        Write-Output "Remove ($($container.Image)) Container $($container.ID)"
        docker container stop $($container.ID)
        docker container rm $($container.ID)
    }
}
Write-Output "Docker remove image $testImage"
docker image rm $testImage
